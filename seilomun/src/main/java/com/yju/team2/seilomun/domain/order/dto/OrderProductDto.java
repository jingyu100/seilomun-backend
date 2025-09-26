package com.yju.team2.seilomun.domain.order.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDto {
    @NotNull
    private Long productId;

    @NotNull @Positive
    private Integer quantity;

    @NotNull @PositiveOrZero
    private Integer price;

    @NotNull @Min(0)
    private Integer currentDiscountRate;
}
