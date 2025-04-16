package com.yju.team2.seilomun.dto;

import com.yju.team2.seilomun.domain.order.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    @NotBlank
    private Integer totalAmount;
    private Integer usedPoints;
    private String memo;
    @NotEmpty
    private Character isDelivery;
    @NotEmpty
    private String deliveryAddress;
    @NotBlank
    private Long productId;
    @NotBlank
    private Integer quantity;
    @NotBlank
    private Integer price;
    @NotBlank
    private Integer currentDiscountRate;
    @NotEmpty
    private String payType; // 결제 타입 - 카드/현금/포인트
    @NotEmpty
    private String orderName; // 주문명
    private String yourSuccessUrl; // 성공 시 리다이렉트 될 URL
    private String yourFailUrl; // 실패 시 리다이렉트 될 URL

}
