package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.domain.order.dto.OrderItemDto;
import com.yju.team2.seilomun.domain.order.entity.OrderItem;
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
    String orderName;
    List<OrderItemDto> orderItems;
    Integer totalAmount;
    Integer usedPoint;
    String address;
    // 배달 요청
    String deliveryRequest;
}
