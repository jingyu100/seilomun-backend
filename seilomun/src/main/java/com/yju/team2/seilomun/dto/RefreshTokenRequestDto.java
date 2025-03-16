package com.yju.team2.seilomun.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {

    private String username;
    private String userType; // "SELLER" 또는 "CUSTOMER"

}