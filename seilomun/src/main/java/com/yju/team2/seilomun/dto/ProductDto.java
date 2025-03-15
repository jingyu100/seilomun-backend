package com.yju.team2.seilomun.dto;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDto {
    private Long id;
    private String name;
    private String category;
    private String description;
    private String thumbnailUrl;
    private Integer originalPrice;
    private Integer discountedPrice;
    private Integer stockQuantity;
    private LocalDateTime expiryDate;
    private Character status;
    private Integer minDiscountRate;
    private Integer maxDiscountRate;
    private Integer currentDiscountRate;
    private LocalDateTime createdAt;
    private List<String> photoUrl;

    public static ProductDto fromEntity(Product product)
    {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .originalPrice(product.getOriginalPrice())
                .discountedPrice(product.getDiscountPrice())
                .stockQuantity(product.getStockQuantity())
                .expiryDate(product.getExpiryDate())
                .status(product.getStatus())
                .minDiscountRate(product.getMinDiscountRate())
                .maxDiscountRate(product.getMaxDiscountRate())
                .currentDiscountRate(product.getCurrentDiscountRate())
                .createdAt(product.getCreatedAt())
                .photoUrl(product.getProductPhotos().stream()
                        .map(ProductPhoto::getPhotoUrl)
                        .toList())
                .build();
    }
}
