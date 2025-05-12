package com.yju.team2.seilomun.domain.product.service;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductCategory;
import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.product.repository.ProductCategoryRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductPhotoRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductSearchRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.domain.product.dto.ProductDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final ProductSearchRepository productSearchRepository;

    private static final String DISCOUNT_RATE_KEY = "Product:currentDiscountRate";
    private static final String DISCOUNT_PRICE_KEY = "Product:discountPrice";
    private static final long CACHE_EXPIRATION_SECONDS = 30 * 60; // 30분 캐싱

    //할인율 조회
    public Integer getCurrentDiscountRate(Long id) {
        String redisKey = DISCOUNT_RATE_KEY + id;

        String cacheRate = redisTemplate.opsForValue().get(redisKey);

        if (cacheRate != null) {
            return Integer.parseInt(cacheRate);
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다"));

        Integer currentDiscountRate = calculateDiscountRate(id);

        //레디스에서 조회
        redisTemplate.opsForValue().set(redisKey, String.valueOf(currentDiscountRate), CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);

        return currentDiscountRate;
    }

    //할인율 계산메서드
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

        Integer discountRate = calculateDiscountRate(id);

        return ProductDto.fromEntity(product, discountRate);
    }

    // 상품 등록
    public ProductDto createProductDto(ProductDto productDto, String sellerEmail) {

        Seller seller = sellerRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다"));

        log.info("상품 등록: 판매자 ID {}, 상품명 {}", seller.getId(), productDto.getName());

        ProductCategory productCategory = productCategoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 카테고리입니다."));

        Product product = productRepository.save(Product.builder()
                .name(productDto.getName())
                .description(productDto.getDescription())
                .originalPrice(productDto.getOriginalPrice())
                .stockQuantity(productDto.getStockQuantity())
                .expiryDate(productDto.getExpiryDate())
                .minDiscountRate((productDto.getMinDiscountRate()))
                .status('1') // 상품등록하면 1
                .maxDiscountRate((productDto.getMaxDiscountRate()))
                .createdAt(productDto.getCreatedAt())
                .seller(seller)
                .productCategory(productCategory)
                .build());

        Integer currentDiscountRate = getCurrentDiscountRate(product.getId());

        // Elasticsearch에 저장
        ProductDocument productDoc = ProductDocument.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .sellerId(product.getSeller().getId())
                .createdAt(productDto.getCreatedAt()) // 추가
                .expiryDate(productDto.getExpiryDate()) // 추가
                .averageRating(0.0) // 기본값 설정 (필요하면 별도 로직 추가)
                .build();

        productSearchRepository.save(productDoc);


        ProductDto productdto = ProductDto.fromEntity(product, currentDiscountRate);


        if (productDto.getPhotoUrl() != null && !productDto.getPhotoUrl().isEmpty()) {
            productDto.getPhotoUrl().forEach(url -> {
                productPhotoRepository.save(ProductPhoto.builder()
                        .product(product)
                        .photoUrl(url)
                        .build());
            });
        }

        return productdto;
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

        // ElasticSearch에서 삭제
        productSearchRepository.deleteById(String.valueOf(product.getId()));

        // 상품 사진 삭제
        productPhotoRepository.deleteByProduct(product);

        // 레디스 삭제
        redisTemplate.delete(DISCOUNT_RATE_KEY + id);
        redisTemplate.delete(DISCOUNT_RATE_KEY + id);

        // 상품 삭제
        productRepository.delete(product);
    }

    //상품 수정
    public ProductDto updateProductDto(Long id, ProductDto productDto, String sellerEmail) {

        Seller seller = sellerRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다"));

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("수정 할 권한이 없습니다");
        }

        ProductCategory productCategory = productCategoryRepository.findById(productDto.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));

        product.updateProudct(productDto,productCategory);

        // ElasticSearch 문서 업데이트
        ProductDocument productDoc = ProductDocument.from(product);
        productSearchRepository.save(productDoc);


        String currentDiscountRateKey = DISCOUNT_RATE_KEY + id;
        String discountPriceKey = DISCOUNT_PRICE_KEY + id;

        redisTemplate.delete(currentDiscountRateKey);
        redisTemplate.delete(discountPriceKey);

        productRepository.save(product);

        Integer currentDiscountRate = getCurrentDiscountRate(id);

        return ProductDto.fromEntity(product, currentDiscountRate);
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
                product.updateProudct(productDto,productCategory);
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
}
