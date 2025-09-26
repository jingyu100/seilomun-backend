package com.yju.team2.seilomun.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
// OrderDto.java
public class OrderDto {
    @NotNull
    @PositiveOrZero
    private Integer usedPoints;

    private String memo;

    // Character → String (가장 중요)
    @NotNull
    private String isDelivery; // "Y" / "N"

    private String deliveryAddress;

    @NotEmpty @Valid
    private List<OrderProductDto> orderProducts;

    @NotBlank
    private String payType;

    @NotBlank
    private String orderName;

    private String yourSuccessUrl;
    private String yourFailUrl;
}
