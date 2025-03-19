package com.yju.team2.seilomun.domain.product.service;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.product.repository.ProductPhotoRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductSearchRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.dto.ProductDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedisTemplate<String, String> redisTemplate;
    private final SellerRepository sellerRepository;
    private final ProductSearchRepository productSearchRepository;

    private static final String DISCOUNT_RATE_KEY = "Product:currentDiscountRate";
    private static final String DISCOUNT_PRICE_KEY = "Product:discountPrice";
    private static final long CACHE_EXPIRATION_SECONDS = 30 * 60; // 30분 캐싱

    //할인율 조회
//    public Integer getCurrentDiscountRate(Long id) {
//        String redisKey = DISCOUNT_RATE_KEY + id;
//
//        String cacheRate = redisTemplate.opsForValue().get(redisKey);
//
//        if (cacheRate != null) {
//            return Integer.parseInt(cacheRate);
//        }
//
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다"));
//
//        Integer currentDiscountRate = product.getCurrentDiscountRate(id);
//
//        redisTemplate.opsForValue().set(redisKey, String.valueOf(currentDiscountRate), CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
//
//        return currentDiscountRate;
//    }

    // 30분 마다 할인율 조정
//    @Scheduled(fixedRate = 30 * 60 * 1000)
//    @Transactional
//    public void updateDiscountPrices() {
//        List<Product> products = productRepository.findAll();
//        for (Product product : products) {
//            String currentDiscountRateKey = DISCOUNT_RATE_KEY + product.getId();
//            String discountPriceKey = DISCOUNT_PRICE_KEY + product.getId();
//
//            Integer currentDiscountRate = product.calculateDiscountRate();
//
//            redisTemplate.opsForValue().set(currentDiscountRateKey, String.valueOf(currentDiscountRate), CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
//        }
//    }



    // 상품 목록 조회
//    public List<ProductDto> getAllProducts() {
//        return productRepository.findAll().stream()
//                .map(product -> {
//                    return ProductDto.fromEntity(product);
//                })
//                .toList();
//    }

    // 상품 상세 조회
    public ProductDto getProductById(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    return ProductDto.fromEntity(product);
                })
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));
    }

    // 상품 등록
    public ProductDto createProductDto(ProductDto productDto, String sellerEmail) {

        Seller seller = sellerRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다"));

        log.info("상품 등록: 판매자 ID {}, 상품명 {}", seller.getId(), productDto.getName());

        Product product = productRepository.save(Product.builder()
                .name(productDto.getName())
                .description(productDto.getDescription())
                .thumbnailUrl(productDto.getThumbnailUrl())
                .originalPrice(productDto.getOriginalPrice())
                .stockQuantity(productDto.getStockQuantity())
                .expiryDate(productDto.getExpiryDate())
                .minDiscountRate((productDto.getMinDiscountRate()))
                .status('1') // 상품등록하면 1
                .maxDiscountRate((productDto.getMaxDiscountRate()))
                .createdAt(productDto.getCreatedAt())
                .seller(seller)
                .build());

        // Elasticsearch에 저장
        ProductDocument productDoc = ProductDocument.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .originalPrice(product.getOriginalPrice())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .sellerId(product.getSeller().getId())
                .build();

        productSearchRepository.save(productDoc);


        ProductDto productdto = ProductDto.fromEntity(product);


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

        productPhotoRepository.deleteByProduct(product);
        productRepository.delete(product);
    }

    //상품 수정
//    public ProductDto updateProductDto(Long id, ProductDto productDto) {
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));
//
//        product.setName(productDto.getName());
//        product.setCategory(productDto.getCategory());
//        product.setDescription(productDto.getDescription());
//        product.setThumbnailUrl(productDto.getThumbnailUrl());
//        product.setOriginalPrice(productDto.getOriginalPrice());
//        product.setStockQuantity(productDto.getStockQuantity());
//        product.setExpiryDate(productDto.getExpiryDate());
//        product.setMinDiscountRate(productDto.getMinDiscountRate());
//        product.setMaxDiscountRate(productDto.getMaxDiscountRate());
//        product.setCreatedAt(productDto.getCreatedAt());
//
//        Integer currentDiscountRate = product.calculateDiscountRate();
//        Integer DiscountPrice = product.calculateDiscountPrice(productDto.getOriginalPrice(), currentDiscountRate);
//
//        ProductDto updateProductDto = ProductDto.fromEntity(product, currentDiscountRate, DiscountPrice);
//
//        if (productDto.getPhotoUrl() != null && !productDto.getPhotoUrl().isEmpty()) {
//            productPhotoRepository.deleteByProduct(product);
//
//            productDto.getPhotoUrl().forEach(url -> {
//                productPhotoRepository.save(ProductPhoto.builder()
//                        .product(product)
//                        .photoUrl(url)
//                        .build());
//            });
//        }
//
//        return updateProductDto;
//    }

    // 유통기한이 현재시간이 되면 상태변화 메서드
    public void updateExpiredProductStatus(ProductDto productDto) {
        LocalDateTime now = LocalDateTime.now();
        List<Product> expiredProducts = productRepository.findByExpiryDateBeforeAndStatusNot(now, '0');
        productDto.setStatus('0');
        if (!expiredProducts.isEmpty()) {
            for (Product product : expiredProducts) {
                // 유통기한 다되면 상태를 변화 지금은 임시로 0 차후에 뭘로 할지 상의
                product.updateProudct(productDto);
                productRepository.save(product);

                String currentDiscountRateKey = DISCOUNT_RATE_KEY + product.getId();
                String discountPriceKey = DISCOUNT_PRICE_KEY + product.getId();
                redisTemplate.delete(currentDiscountRateKey);
                redisTemplate.delete(discountPriceKey);
            }
        }
    }

    // Elasticsearch에서 검색
    public List<ProductDocument> searchProducts(String keyword) {
        return productSearchRepository.findByNameContaining(keyword);
    }

}
