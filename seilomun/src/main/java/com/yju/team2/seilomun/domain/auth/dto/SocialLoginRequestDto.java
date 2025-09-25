package com.yju.team2.seilomun.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class    SocialLoginRequestDto {

    @NotBlank(message = "provider는 필수입니다.")
    @Pattern(regexp = "^(KAKAO|NAVER|GOOGLE|APPLE)$",
            message = "지원하지 않는 provider 입니다. (KAKAO|NAVER|GOOGLE|APPLE)")
    private String provider;

    // KAKAO / NAVER
    private String code;

    // GOOGLE / APPLE
    private String idToken;

    // KAKAO / NAVER 교환 시 필요
    private String redirectUri;

    public boolean isCodeProvider() {
        return "KAKAO".equalsIgnoreCase(provider) || "NAVER".equalsIgnoreCase(provider);
    }

    public boolean isIdTokenProvider() {
        return "GOOGLE".equalsIgnoreCase(provider) || "APPLE".equalsIgnoreCase(provider);
    }
}
