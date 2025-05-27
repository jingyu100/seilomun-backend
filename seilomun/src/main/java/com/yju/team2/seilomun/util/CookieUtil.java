package com.yju.team2.seilomun.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

public class CookieUtil {

    // AccessToken 쿠키 생성
    public static ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(Duration.ofMinutes(30))  // 30분
                .path("/")
                .build();
    }

    // RefreshToken 쿠키 생성
    public static ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(14))  // 14일
                .path("/")
                .build();
    }

    // 삭제용 만료된 AccessToken 쿠키 생성
    public static ResponseCookie createExpiredAccessTokenCookie() {
        return ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(0)  // 즉시 만료
                .path("/")
                .build();
    }

    // 삭제용 만료된 RefreshToken 쿠키 생성
    public static ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(0)  // 즉시 만료
                .path("/")
                .build();
    }
}