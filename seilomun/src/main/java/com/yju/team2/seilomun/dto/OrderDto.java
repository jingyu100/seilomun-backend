package com.yju.team2.seilomun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
