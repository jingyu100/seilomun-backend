package com.yju.team2.seilomun.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPhotoDto {

    private Long id;
    private String photoUrl;
}
