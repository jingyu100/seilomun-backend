package com.yju.team2.seilomun.dto;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductCategory;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDto {
    private String name;
    private String description;
    private String thumbnailUrl;
    private Integer originalPrice;
    private Integer stockQuantity;
    private LocalDateTime expiryDate;
    private Character status;
    private Integer minDiscountRate;
    private Integer maxDiscountRate;
    private LocalDateTime createdAt;
    private List<String> photoUrl;
    private Seller seller;
    private ProductCategory productCategory;

    public static ProductDto fromEntity(Product product) {
        return ProductDto.builder()
                .name(product.getName())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .originalPrice(product.getOriginalPrice())
                .stockQuantity(product.getStockQuantity())
                .expiryDate(product.getExpiryDate())
                .status(product.getStatus())
                .minDiscountRate(product.getMinDiscountRate())
                .maxDiscountRate(product.getMaxDiscountRate())
                .createdAt(product.getCreatedAt())
                .seller(product.getSeller())
                .productCategory(product.getProductCategory())
                .build();
    }
}
