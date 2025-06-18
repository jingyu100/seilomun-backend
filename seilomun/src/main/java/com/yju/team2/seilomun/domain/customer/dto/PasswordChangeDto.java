package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeDto {

    @NotBlank(message = "현재 비밀번호를 필수입니다")
    private String currentPassword;
    @ValidPassword
    private String newPassword;
    @ValidPassword
    private String confirmPassword;
    
    //현재 비밀번호와 새로운 비밀번호 확인
    public boolean isValidPassword() {
        return this.newPassword != null && this.newPassword.equals(this.confirmPassword);
    }

    //새로운 비밀번호랑 비밀번호 확인
    public boolean isNewPasswordValid() {
        return (this.newPassword.equals(this.confirmPassword));
    }

    public boolean hasPasswordChangeRequest() {
        return this.newPassword != null && !this.newPassword.isEmpty();
    }
}
