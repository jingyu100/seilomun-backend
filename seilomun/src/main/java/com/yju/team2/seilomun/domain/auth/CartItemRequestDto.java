package com.yju.team2.seilomun.domain.auth;

import lombok.Data;

@Data
public class CartItemRequestDto {

    private Long productId;
    private Integer quantity;
}
