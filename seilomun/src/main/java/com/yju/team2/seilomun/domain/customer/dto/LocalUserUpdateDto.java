package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocalUserUpdateDto {

    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String nickname;

    @NotBlank
    private String phone;

    @NotNull
    private Character gender;

    @NotBlank
    private String birthDate;

    private MultipartFile profileImage;
    private String profileImageUrl;

}
