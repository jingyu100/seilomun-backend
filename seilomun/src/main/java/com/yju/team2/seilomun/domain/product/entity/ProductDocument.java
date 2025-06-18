package com.yju.team2.seilomun.domain.product.entity;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Document(indexName = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Integer)
    private Integer originalPrice;

    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    @Field(type = FieldType.Keyword)
    private Character status;

    @Field(type = FieldType.Long)
    private Long sellerId;

    @Field(type = FieldType.Long)
    private Long categoryId;

    // 추가 필드
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime expiryDate;

    @Field(type = FieldType.Double)
    private Double averageRating;

    public static ProductDocument from(Product product) {
        return ProductDocument.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .sellerId(product.getSeller().getId())
                .categoryId(product.getProductCategory().getId())
                .createdAt(product.getCreatedAt())
                .expiryDate(product.getExpiryDate())
                // averageRating은 별도로 계산 필요
                .build();
    }
}