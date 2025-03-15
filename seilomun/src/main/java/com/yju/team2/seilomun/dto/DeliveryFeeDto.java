package com.yju.team2.seilomun.dto;

import com.yju.team2.seilomun.domain.seller.entity.Seller;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryFeeDto {
    private Long id;
    private Integer ordersMoney;
    private Integer deliveryTip;
}
