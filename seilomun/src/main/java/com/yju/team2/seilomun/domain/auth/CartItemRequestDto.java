package com.yju.team2.seilomun.domain.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartItemRequestDto {

    @NotBlank
    private Long productId;

    @NotBlank
    private Integer quantity;
}
