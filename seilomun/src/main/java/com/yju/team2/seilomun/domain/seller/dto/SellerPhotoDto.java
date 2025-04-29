package com.yju.team2.seilomun.domain.seller.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerPhotoDto {

    @NotEmpty
    private String photoUrl;
}
