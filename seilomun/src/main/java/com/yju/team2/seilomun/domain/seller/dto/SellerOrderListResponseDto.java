package com.yju.team2.seilomun.domain.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerOrderListResponseDto {
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private Integer totalAmount;
    private LocalDateTime orderDate;
    private List<String> orderItems;
    private String photoUrl;
    private Character orderStatus;
    private Character isDelivery;
}