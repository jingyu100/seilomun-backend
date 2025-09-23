package com.yju.team2.seilomun.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public class CookieUtil {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            return Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(name))
                    .findFirst();
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(name))
                    .forEach(cookie -> {
                        cookie.setValue("");
                        cookie.setPath("/");
                        cookie.setMaxAge(0);
                        response.addCookie(cookie);
                    });
        }
    }

    // AccessToken 쿠키 생성
    public static ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(Duration.ofHours(2))  // 2시간
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