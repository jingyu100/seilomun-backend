package com.yju.team2.seilomun.domain.product.service;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
//import com.yju.team2.seilomun.domain.product.repository.CurrentDiscountRateRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductPhotoRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPhotoRepository productPhotoRepository;
//    private final CurrentDiscountRateRepository currentDiscountRateRepository;

    //할인율 조회

//    public Integer getCurrentDiscountRate(Long id) {
//        Integer CurrentDiscountRate = currentDiscountRateRepository.getDiscounRate(id);
//
//        if(CurrentDiscountRate != null) {
//            return CurrentDiscountRate;
//        }
//
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("할인율 조회하지 못했습니다"));
//
////        CurrentDiscountRate = calculateDiscountRate(product);
//
//        currentDiscountRateRepository.saveCurrentDiscountRate(id, CurrentDiscountRate);
//
//        return CurrentDiscountRate;
//    }
    
    // 할인율 계산
//    private Integer calculateDiscountRate(Product product) {
//        int minDiscountRate = product.getMinDiscountRate();
//        int maxDiscountRate = product.getMaxDiscountRate();
//        LocalDateTime expiryDate = product.getExpiryDate();
//        long day = ChronoUnit.DAYS.between(expiryDate, LocalDateTime.now());
//
////        int currentDiscountRate = minDiscountRate + ((expiryDate - day) / expiryDate) * (maxDiscountRate-minDiscountRate);
////        return currentDiscountRate;
//        return
//    }

    // 상품 목록 조회
    public List<ProductDto> getAllProudcts() {
        return productRepository.findAll().stream()
                .map(ProductDto::fromEntity)
                .toList();
    }

    // 상품 상세 조회
    public ProductDto getProductById(Long id){
        return productRepository.findById(id)
                .map(ProductDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));
    }

    // 상품 등록
    public ProductDto createProductDto(ProductDto productDto)
    {
        Product product = productRepository.save(Product.builder()
                .name(productDto.getName())
                .category(productDto.getCategory())
                .description(productDto.getDescription())
                .thumbnailUrl(productDto.getThumbnailUrl())
                .originalPrice(productDto.getOriginalPrice())
//                .discountPrice(productDto.getDiscountedPrice())
                .stockQuantity(productDto.getStockQuantity())
                .expiryDate(productDto.getExpiryDate())
                .minDiscountRate((productDto.getMinDiscountRate()))
                .maxDiscountRate((productDto.getMaxDiscountRate()))
//                .currentDiscountRate(productDto.getCurrentDiscountRate())
                .createdAt(productDto.getCreatedAt())
                .build());

        if(productDto.getPhotoUrl() != null && !productDto.getPhotoUrl().isEmpty())
        {
            productDto.getPhotoUrl().forEach(url -> {
                productPhotoRepository.save(ProductPhoto.builder()
                        .product(product)
                        .photoUrl(url)
                        .build());
            });
        }


        return ProductDto.fromEntity(product);
    }

}
