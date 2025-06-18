package com.yju.team2.seilomun.domain.product.service;

import com.yju.team2.seilomun.domain.notification.event.CartProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.event.LikeProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.event.NewProductEvent;
import com.yju.team2.seilomun.domain.notification.event.ProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.service.NotificationService;
import com.yju.team2.seilomun.domain.product.dto.DiscountInfo;
import com.yju.team2.seilomun.domain.product.dto.ProductPhotoDto;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductCategory;
import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.product.repository.ProductCategoryRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductPhotoRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.search.repository.ProductSearchRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.domain.product.dto.ProductDto;
import com.yju.team2.seilomun.domain.upload.service.AWSS3UploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SellerRepository sellerRepository;
    private final ProductIndexService productIndexService;
    private final NotificationService notificationService;
    private final AWSS3UploadService awsS3UploadService;
    private final ProductDiscountService productDiscountService;

    private static final String DISCOUNT_RATE_KEY = "Product:currentDiscountRate";
    private static final String DISCOUNT_PRICE_KEY = "Product:discountPrice";
    private static final long CACHE_EXPIRATION_SECONDS = 30 * 60; // 30분 캐싱

    // 할인율 조회
    public Integer getCurrentDiscountRate(Long id) {
        String redisKey = DISCOUNT_RATE_KEY + id;

        String cacheRate = redisTemplate.opsForValue().get(redisKey);

        if (cacheRate != null) {
            return Integer.parseInt(cacheRate);
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다"));

        Integer currentDiscountRate = calculateDiscountRate(id);

        // 레디스에서 조회
        redisTemplate.opsForValue().set(redisKey, String.valueOf(currentDiscountRate), CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);

        return currentDiscountRate;
    }

    // 할인율 계산메서드
    public Integer calculateDiscountRate(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다"));

        if (product.getExpiryDate() == null || product.getMinDiscountRate() == null || product.getMaxDiscountRate() == null)
            return 0;

        //현재 시간이 만료일을 지났는지 확인
        LocalDateTime now = LocalDateTime.now();

        // 만료일이 지나면 최소할인율
        if (now.isAfter(product.getExpiryDate())) {
            return product.getMinDiscountRate();
        }

        long totalPeriod = Duration.between(product.getCreatedAt(), product.getExpiryDate()).toDays();
        long remainingDays = Duration.between(now, product.getExpiryDate()).toDays();

        if (remainingDays < 0)
            remainingDays = 0;
        if (totalPeriod <= 0)
            return product.getMaxDiscountRate();
        if (totalPeriod <= 3)
            return product.getMaxDiscountRate();

        double ratio = 1.0 - ((double) remainingDays / (double) totalPeriod); // 경과 비율
        int interpolatedRate = (int) Math.round(
                product.getMinDiscountRate() +
                        (product.getMaxDiscountRate() - product.getMinDiscountRate()) * ratio
        );

        log.info("할인율 계산: 전체기간={}일, 남은기간={}일, 경과비율={}, 할인율={}",
                totalPeriod, remainingDays, String.format("%.2f", ratio), interpolatedRate);

        return interpolatedRate;

    }

    // 상품 상세 조회
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다"));

        // 원래 코드
//        Integer discountRate = calculateDiscountRate(id);
//
//        return ProductDto.fromEntity(product, discountRate);

        DiscountInfo discountInfo = productDiscountService.getDiscountInfo(id);

        return ProductDto.fromEntity(product, discountInfo.getDiscountRate(), discountInfo.getDiscountedPrice());
    }

    // 상품 등록
    public ProductDto createProductDto(ProductDto productDto, String sellerEmail, List<MultipartFile> productPhotos) {

        Seller seller = sellerRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다"));

        log.info("상품 등록: 판매자 ID {}, 상품명 {}", seller.getId(), productDto.getName());

        // categoryId null 체크
        if (productDto.getCategoryId() == null) {
            throw new IllegalArgumentException("카테고리 ID는 필수입니다");
        }

        log.info("카테고리 ID: {}", productDto.getCategoryId());

        ProductCategory productCategory = productCategoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 카테고리입니다."));

        log.info("카테고리 조회 완료: {}", productCategory.getCategoryName());

        // null 체크 추가
        LocalDateTime createdAt = productDto.getCreatedAt() != null ? productDto.getCreatedAt() : LocalDateTime.now();
        LocalDateTime expiryDate = productDto.getExpiryDate() != null ? productDto.getExpiryDate() : LocalDateTime.now().plusDays(7);

        Product product = Product.builder()
                .name(productDto.getName())
                .description(productDto.getDescription())
                .originalPrice(productDto.getOriginalPrice())
                .stockQuantity(productDto.getStockQuantity())
                .expiryDate(productDto.getExpiryDate())
                .minDiscountRate((productDto.getMinDiscountRate()))
                .status('1')
                .maxDiscountRate((productDto.getMaxDiscountRate()))
                .createdAt(productDto.getCreatedAt())
                .seller(seller)
                .productCategory(productCategory)  // 이 줄을 추가해야 합니다
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("상품 저장 완료: productId={}", savedProduct.getId());

        Integer currentDiscountRate = getCurrentDiscountRate(savedProduct.getId());


        // ProductIndexService를 사용하여 Elasticsearch에 인덱싱
        try {
            if (productIndexService != null) {
                productIndexService.indexProduct(savedProduct);
                log.info("Elasticsearch 인덱싱 완료");
            }
        } catch (Exception e) {
            // 인덱싱 실패해도 상품 등록은 계속 진행
            log.error("Elasticsearch 인덱싱 실패", e);
        }

        // 즐겨찾기한 고객들에게 알림 전송
        try {
            if (notificationService != null) {
                NewProductEvent productEvent = NewProductEvent.builder()
                        .product(savedProduct)
                        .eventId("NEW_PRODUCT_" + savedProduct.getId())
                        .build();

                notificationService.processNotification(productEvent);
                log.info("상품 등록 알림 전송 완료");
            }
        } catch (Exception e) {
            log.error("상품 등록 알림 전송 실패", e);
            // 알림 전송 실패해도 상품 등록은 계속 진행
        }

        if(productPhotos != null && !productPhotos.isEmpty()) {
            int currentProductPhotoCount = product.getProductPhotos().size();
            if(currentProductPhotoCount + productPhotos.size() > 5) {
                throw new IllegalArgumentException("상품 사진은 최대 5장 까지 등록할 수 있습니다");
            }

            List<ProductPhoto> productPhotoList = uploadAndCreatePhotos(
                    productPhotos,
                    product,
                    5,
                    url -> ProductPhoto.builder()
                            .photoUrl(url)
                            .product(product)
                            .build()
            );
            product.getProductPhotos().addAll(productPhotoList);
        }

        return ProductDto.fromEntity(savedProduct, currentDiscountRate);
    }

    // 상품 삭제
    public void deleteProduct(Long id, String sellerEmail) {

        Seller seller = sellerRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다"));

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("삭제 할 권한이 없습니다");
        }

        // ProductIndexService를 사용하여 Elasticsearch에서 삭제
        productIndexService.deleteProduct(product.getId());

        // 상품 사진 삭제
        productPhotoRepository.deleteByProduct(product);

        // 레디스 삭제
        redisTemplate.delete(DISCOUNT_RATE_KEY + id);
        redisTemplate.delete(DISCOUNT_PRICE_KEY + id);

        // 상품 삭제
        productRepository.delete(product);
    }

    // 상품 수정
    public ProductDto updateProductDto(Long productId, ProductDto productDto, String sellerEmail,List<MultipartFile> productImages) {

        Seller seller = sellerRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("수정 할 권한이 없습니다");
        }

        ProductCategory productCategory = productCategoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));

        if(productDto.getProductPhotos() != null && !productDto.getProductPhotos().isEmpty()) {
            for(ProductPhotoDto photoDto : productDto.getProductPhotos()) {
                ProductPhoto productDelete = productPhotoRepository.findById(photoDto.getId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않은 상품입니다."));

                if(!productDelete.getProduct().getId().equals(product.getId())) {
                    throw new IllegalArgumentException("상품 사진에 대한 권한이 없습니다.");
                }

                productPhotoRepository.delete(productDelete);
                product.getProductPhotos().remove(productDelete);
                log.info("상품 사진이 삭제되었습니다 : {}",photoDto.getId());
            }
        }

        if(productImages != null && !productImages.isEmpty()) {
            int currentProductPhotoCount = product.getProductPhotos().size();
            if(currentProductPhotoCount + productImages.size() > 5) {
                throw new IllegalArgumentException("상품 사진은 최대 5장 까지 등록할 수 있습니다");
            }

            List<ProductPhoto> productPhotoList = uploadAndCreatePhotos(
                    productImages,
                    product,
                    5,
                    url -> ProductPhoto.builder()
                            .photoUrl(url)
                            .product(product)
                            .build()
            );
            product.getProductPhotos().addAll(productPhotoList);
        }

        Character oldStatus = product.getStatus();
        product.updateProudct(productDto, productCategory);
        Product updatedProduct = productRepository.save(product);

        // 상태가 변경되었다면 알림 발생
        if (!oldStatus.equals(updatedProduct.getStatus())) {
            sendProductStatusChangeNotifications(updatedProduct, oldStatus, updatedProduct.getStatus());
        }

        // ProductIndexService를 사용하여 Elasticsearch 문서 업데이트
        productIndexService.indexProduct(updatedProduct);

        // 캐시 삭제
        String currentDiscountRateKey = DISCOUNT_RATE_KEY + productId;
        String discountPriceKey = DISCOUNT_PRICE_KEY + productId;
        redisTemplate.delete(currentDiscountRateKey);
        redisTemplate.delete(discountPriceKey);

        Integer currentDiscountRate = getCurrentDiscountRate(productId);

        return ProductDto.fromEntity(updatedProduct, currentDiscountRate);
    }

    private <T> List<T> uploadAndCreatePhotos(
            List<MultipartFile> files,
            Product product,
            int maxCount,
            Function<String, T> entityCreator
    ) {
        if(files != null && !files.isEmpty()) {
            if(files.size() > maxCount) {
                throw new IllegalArgumentException("사진은 최대 5장 까지 업로드 할 수 있습니다.");
            }
        }

        List<String> photoUrls = new ArrayList<>();
        try {
            photoUrls = awsS3UploadService.uploadFiles(files);
            log.info("사진 업로드 완료 : {}", photoUrls);
        } catch (Exception e) {
            log.info("사진 업로드 실패 : {}", e.getMessage());
            throw new RuntimeException("사진 업로드 실패했습니다.");
        }

        return photoUrls.stream()
                .map(entityCreator)
                .collect(Collectors.toList());
    }

    // 유통기한이 현재시간이 되면 상태변화 메서드
    public void updateExpiredProductStatus(ProductDto productDto) {
        LocalDateTime now = LocalDateTime.now();
        List<Product> expiredProducts = productRepository.findByExpiryDateBeforeAndStatusNot(now, '0');
        productDto.setStatus('0');
        ProductCategory productCategory = productCategoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
        if (!expiredProducts.isEmpty()) {
            for (Product product : expiredProducts) {
                // 유통기한 다되면 상태를 변화 지금은 임시로 0 차후에 뭘로 할지 상의
                product.updateProudct(productDto, productCategory);
                productRepository.save(product);

                String currentDiscountRateKey = DISCOUNT_RATE_KEY + product.getId();
                String discountPriceKey = DISCOUNT_PRICE_KEY + product.getId();
                redisTemplate.delete(currentDiscountRateKey);
                redisTemplate.delete(discountPriceKey);
            }
        }
    }

    public boolean existsById(Long productId) {
        return productRepository.existsById(productId);
    }

    //
    public int getStockQuantity(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));
        return product.getStockQuantity();
    }

    public List<ProductDto> getProducts(Long sellerId) {
        List<Product> products = productRepository.findBySellerId(sellerId);


        return products.stream()
                .map(product -> {
                    Integer currentDiscountRate = getCurrentDiscountRate(product.getId());
                    return ProductDto.fromEntity(product, currentDiscountRate);
                })
                .collect(Collectors.toList());
    }

    // 상태 변경 알림 메서드
    private void sendProductStatusChangeNotifications(Product product, Character oldStatus, Character newStatus) {
        try {
            // 1. 판매자에게 알림
            ProductStatusChangedEvent sellerEvent = ProductStatusChangedEvent.builder()
                    .product(product)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .eventId("PRODUCT_STATUS_" + product.getId())
                    .build();
            notificationService.processNotification(sellerEvent);

            // 2. 좋아요한 고객들에게 알림
            LikeProductStatusChangedEvent likeEvent = LikeProductStatusChangedEvent.builder()
                    .product(product)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .eventId("LIKE_PRODUCT_STATUS_" + product.getId())
                    .build();
            notificationService.processNotification(likeEvent);

            // 3. 장바구니에 담은 고객들에게 알림
            CartProductStatusChangedEvent cartEvent = CartProductStatusChangedEvent.builder()
                    .product(product)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .eventId("CART_PRODUCT_STATUS_" + product.getId())
                    .build();
            notificationService.processNotification(cartEvent);

        } catch (Exception e) {
            log.error("상품 상태 변경 알림 전송 실패: productId={}", product.getId(), e);
        }
    }

    public Long getSellerIdByProductId(Long productId) {
        System.out.println(productId);
        Product findedProduct = productRepository.findById(productId).orElse(null);
        Seller byId = sellerRepository.findByProducts(findedProduct).orElse(null);
        return byId.getId();
    }

    public String getSellerNameById(Long existingSellerId) {
        Seller byId = sellerRepository.findById(existingSellerId).orElse(null);
        return byId.getStoreName();
    }
}
