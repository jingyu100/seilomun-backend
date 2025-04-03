package com.yju.team2.seilomun.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WishProductDto {
    private Long productId;
    private Long wishId;
    private String name;
    private String description;
    private Integer originalPrice;
    private Integer discountPrice;
    private Integer currentDiscountRate;
    private LocalDateTime expiryDate;
    private String storeAddress;
    private String photoUrl;
    private Character status;
}
