package com.yju.team2.seilomun.domain.search.dto;

import com.yju.team2.seilomun.domain.product.dto.DiscountInfo;
import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchDto {
    private String id;
    private String name;
    private String description;
    private Integer originalPrice;
    private Integer discountRate;
    private Integer discountedPrice;
    private String thumbnailUrl;
    private Integer stockQuantity;
    private Character status;
    private Long sellerId;
    private LocalDateTime createdAt;
    private LocalDateTime expiryDate;
    private Double averageRating;

    // 생성자
    public ProductSearchDto(ProductDocument document, Integer discountRate, Integer discountedPrice) {
        this.id = document.getId();
        this.name = document.getName();
        this.description = document.getDescription();
        this.originalPrice = document.getOriginalPrice();
        this.discountRate = discountRate;
        this.discountedPrice = discountedPrice;
        this.thumbnailUrl = document.getThumbnailUrl();
        this.stockQuantity = document.getStockQuantity();
        this.status = document.getStatus().charAt(0);
        this.sellerId = document.getSellerId();
        this.createdAt = document.getCreatedAt();
        this.expiryDate = document.getExpiryDate();
        this.averageRating = document.getAverageRating();
    }

}