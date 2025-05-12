package com.yju.team2.seilomun.domain.seller.entity;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Document(indexName = "sellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String storeName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String storeDescription;

    @Field(type = FieldType.Text)
    private String addressDetail;

    @Field(type = FieldType.Keyword)
    private String phone;

    @Field(type = FieldType.Keyword)
    private String operatingHours;

    @Field(type = FieldType.Keyword)
    private Character deliveryAvailable;

    @Field(type = FieldType.Float)
    private Float rating;

    @Field(type = FieldType.Keyword)
    private Character isOpen;

    @Field(type = FieldType.Keyword)
    private Character status;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    public static SellerDocument from(Seller seller) {
        String categoryName = seller.getSellerCategory() != null ?
                seller.getSellerCategory().getCategoryName() : null;

        return SellerDocument.builder()
                .id(seller.getId().toString())
                .storeName(seller.getStoreName())
                .storeDescription(seller.getStoreDescription())
                .addressDetail(seller.getAddressDetail())
                .phone(seller.getPhone())
                .operatingHours(seller.getOperatingHours())
                .deliveryAvailable(seller.getDeliveryAvailable())
                .rating(seller.getRating())
                .isOpen(seller.getIsOpen())
                .status(seller.getStatus())
                .categoryName(categoryName)
                .createdAt(seller.getCreatedAt())
                .build();
    }
}