package com.yju.team2.seilomun.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusDto {
    private boolean isAvailable;
    private String status;
    private String operatingHours;
}
