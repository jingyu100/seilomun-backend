package com.yju.team2.seilomun.domain.auth.dto;

import com.yju.team2.seilomun.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

    @Email
    private String email;

    @ValidPassword
    private String password;

    @NotEmpty
    private String userType; // "CUSTOMER" 또는 "SELLER"
}
