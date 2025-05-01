package com.yju.team2.seilomun.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {
    private String productName;
    private LocalDateTime expiryDate;
    private Integer quantity;
    private Integer unitPrice;
    private Integer discountRate;
    private String photoUrl;
}