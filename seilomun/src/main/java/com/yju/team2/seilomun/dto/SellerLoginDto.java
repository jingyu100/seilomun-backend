package com.yju.team2.seilomun.dto;

import com.yju.team2.seilomun.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerLoginDto {
    @Email
    private String email;

    @ValidPassword
    private String password;
}
