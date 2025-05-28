package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderListResponseDto {
    private Long orderId;
    private String sellerName;
    private Integer totalAmount;
    private LocalDateTime orderDate;
    private List<String> orderItems;
    private String photoUrl;
    private Character orderStatus;
}
