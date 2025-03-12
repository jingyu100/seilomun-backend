package com.yju.team2.seilomun.dto;

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
    @NotEmpty
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
