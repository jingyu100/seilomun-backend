package com.yju.team2.seilomun.domain.seller.dto;

import com.yju.team2.seilomun.domain.seller.entity.SellerCategoryEntity;
import com.yju.team2.seilomun.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
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

    @ValidPassword
    private String password;

    @NotEmpty
    private String storeName;

    @NotEmpty
    private Long categoryId;

    //우편번호는 비워놓았습니다.
    @NotEmpty
    private String phone;

    @NotEmpty
    private String addressDetail;

}
