package com.yju.team2.seilomun.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDto {
    @NotBlank
    private Long productId;
    @NotBlank
    private Integer quantity;
    private Integer price;
}
