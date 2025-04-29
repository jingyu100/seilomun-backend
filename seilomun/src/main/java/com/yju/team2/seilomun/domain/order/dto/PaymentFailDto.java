package com.yju.team2.seilomun.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailDto {
    String errorCode;
    String errorMessage;
    String orderId;
}
