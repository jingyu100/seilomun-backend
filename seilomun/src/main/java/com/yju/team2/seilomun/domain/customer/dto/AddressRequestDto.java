package com.yju.team2.seilomun.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequestDto {

    private String postCode;
    private String addressDetail;
    private Character addressMain;
    private String label;
}
