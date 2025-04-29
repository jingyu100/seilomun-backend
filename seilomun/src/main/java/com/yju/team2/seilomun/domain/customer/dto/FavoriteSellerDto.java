package com.yju.team2.seilomun.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteSellerDto {
    private Long id;
    private String storeName;
    private String addressDetail;
    private Float rating;
}
