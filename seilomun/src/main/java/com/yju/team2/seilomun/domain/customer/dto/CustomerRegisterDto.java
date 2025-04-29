package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerRegisterDto {
//    NotBlank 고려
//    @NotEmpty
    @ValidPassword
    private String password;

    @NotEmpty
    private String nickname;

    @NotEmpty
    private String name;

    @Email
    private String email;

    @NotEmpty
    private String phone;

    @NotEmpty
    private Character gender;

    @NotEmpty
    private String address;

    @NotEmpty
    private String birthdate;
}
