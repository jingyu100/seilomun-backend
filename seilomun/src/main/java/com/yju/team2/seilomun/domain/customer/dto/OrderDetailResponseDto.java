package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.domain.order.dto.OrderItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailResponseDto {
    String storeName;
    LocalDateTime orderDate;
    String orderNumber;
    List<OrderItemDto> orderItems;
    Integer totalAmount;
    Integer usedPoint;
    Integer deliveryFee;
    String address;
    // 배달 요청
    String deliveryRequest;
}
