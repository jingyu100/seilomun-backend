package com.yju.team2.seilomun.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerRegisterDto {

    @Email
    private String email;

    //사업자 등록번호 인증은 추후에
    @NotEmpty
    private String businessNumber;

    @NotEmpty
    private String password;

    @NotEmpty
    private String storeName;

    //우편번호는 비워놓았습니다.
    @NotEmpty
    private String phone;

    @NotEmpty
    private String addressDetail;

}
