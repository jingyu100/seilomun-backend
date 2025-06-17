package com.yju.team2.seilomun.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountInfo {
    private Long productId;
    private Integer originalPrice;
    private Integer discountRate;
    private Integer discountedPrice;
    private LocalDateTime calculatedAt;
}
