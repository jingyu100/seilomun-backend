package com.yju.team2.seilomun.domain.seller.dto;

import com.yju.team2.seilomun.domain.order.dto.OrderItemDto;
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
public class SellerOrderDetailResponseDto {
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String customerPhone;
    private LocalDateTime orderDate;
    private List<OrderItemDto> orderItems;
    private Integer totalAmount;
    private Integer usedPoints;
    private Integer deliveryFee;
    private Character isDelivery;
    private String deliveryAddress;
    private String memo;
    private Character orderStatus;
    private String paymentStatus;
}