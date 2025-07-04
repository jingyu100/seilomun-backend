package com.yju.team2.seilomun.domain.product.dto;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductCategory;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.seller.dto.SellerInformationDto;
import com.yju.team2.seilomun.domain.seller.dto.SellerPhotoDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private Integer originalPrice;
    private Integer discountPrice;
    private Integer stockQuantity;
    private LocalDateTime expiryDate;
    private Character status;
    private Integer minDiscountRate;
    private Integer maxDiscountRate;
    private Integer currentDiscountRate;
    private LocalDateTime createdAt;
    private Long sellerId;
    private SellerInformationDto seller;
    private Long categoryId;
    private List<String> productPhotoUrl;
    private List<Long> productPhotoIds;

    // 할인 정보를 포함한 팩토리 메서드
    public static ProductDto fromEntity(Product product, Integer currentDiscountRate, Integer discountPrice) {
        List<String> productPhotos = product.getProductPhotos().stream()
                .map(ProductPhoto::getPhotoUrl)
                .collect(Collectors.toList());

        List<Long> productPhotoIds = product.getProductPhotos().stream()
                .map(ProductPhoto::getId)
                .collect(Collectors.toList());

        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .discountPrice(discountPrice)
                .stockQuantity(product.getStockQuantity())
                .expiryDate(product.getExpiryDate())
                .status(product.getStatus())
                .minDiscountRate(product.getMinDiscountRate())
                .maxDiscountRate(product.getMaxDiscountRate())
                .currentDiscountRate(currentDiscountRate)
                .createdAt(product.getCreatedAt())
                .sellerId(product.getSeller().getId())
                .seller(SellerInformationDto.toDto(product.getSeller()))
                .categoryId(product.getProductCategory().getId())
                .productPhotoUrl(productPhotos)
                .productPhotoIds(productPhotoIds)
                .build();
    }

    // 기존 호환성을 위한 메서드 (deprecated)
    @Deprecated
    public static ProductDto fromEntity(Product product, Integer currentDiscountRate) {
        Integer discountedPrice = product.getOriginalPrice() * (100 - currentDiscountRate) / 100;
        return fromEntity(product, currentDiscountRate, discountedPrice);
    }

//    public static ProductDto fromEntity(Product product, Integer currentDiscountRate) {
//        return ProductDto.builder()
//                .id(product.getId())
//                .name(product.getName())
//                .description(product.getDescription())
//                .originalPrice(product.getOriginalPrice())
//                .discountPrice(product.getOriginalPrice() * (100 - currentDiscountRate) / 100)
//                .stockQuantity(product.getStockQuantity())
//                .expiryDate(product.getExpiryDate())
//                .status(product.getStatus())
//                .minDiscountRate(product.getMinDiscountRate())
//                .maxDiscountRate(product.getMaxDiscountRate())
//                .currentDiscountRate(currentDiscountRate)
//                .createdAt(product.getCreatedAt())
//                .seller(SellerInformationDto.toDto(product.getSeller()))
//                .categoryId(product.getProductCategory().getId())
//                .photoUrl(product.getProductPhotos().stream()
//                        .map(ProductPhoto::getPhotoUrl)
//                        .collect(Collectors.toList()))
//                .build();
//    }
}
