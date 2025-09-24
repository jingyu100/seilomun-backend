package com.yju.team2.seilomun.domain.customer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React Native 앱으로부터 카카오 accessToken을 받기 위한 DTO(Data Transfer Object)
 */
@Getter
@Setter
@NoArgsConstructor
public class KakaoLoginRequestDto {
    // 앱이 보내주는 JSON 형식의 {"accessToken": "..."} 값을 이 필드에 자동으로 담게 됩니다.
    private String accessToken;
}
