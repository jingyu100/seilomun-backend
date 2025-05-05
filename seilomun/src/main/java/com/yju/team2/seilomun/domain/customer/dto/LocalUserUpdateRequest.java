package com.yju.team2.seilomun.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalUserUpdateRequest {
    private LocalUserUpdateDto updateDto;
    private PasswordChangeDto passwordChangeDto;
}
