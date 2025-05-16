package com.yju.team2.seilomun.domain.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerStatusUpdateDto {
    private Character isOpen; // '1': 영업중, '0': 영업종료, '2': 브레이크타임
}
