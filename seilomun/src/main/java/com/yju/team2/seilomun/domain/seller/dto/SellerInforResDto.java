package com.yju.team2.seilomun.domain.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerInforResDto {
    private Long sellerId;
    private String sellerName;
}
