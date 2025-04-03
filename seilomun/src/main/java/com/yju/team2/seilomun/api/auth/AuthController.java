package com.yju.team2.seilomun.api.auth;

import com.yju.team2.seilomun.domain.auth.JwtUserDetailsService;
import com.yju.team2.seilomun.domain.auth.OauthService;
import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.RefreshTokenRequestDto;
import com.yju.team2.seilomun.dto.UsernameRequest;
import com.yju.team2.seilomun.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "인증 관리", description = "토큰 갱신 및 인증 관련 API")
public class AuthController {

    private final JwtUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final OauthService oauthService;


    // RefreshToken을 사용하여 새로운 AccessToken을 발급
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseJson> refreshToken(@RequestBody RefreshTokenRequestDto request, HttpServletResponse response) {

        String username = request.getUsername();
        String userType = request.getUserType();

        log.info("토큰 갱신 요청 - 사용자: {}, 유형: {}", username, userType);

        // Redis에서 저장된 RefreshToken 조회
        String refreshToken = refreshTokenService.getRefreshToken(username);

        System.out.println("refreshToken : " + refreshToken);
        if (refreshToken != null && refreshTokenService.validateRefreshToken(refreshToken, username)) {

            // 토큰에서 userType 검증
            String tokenUserType = jwtUtil.extractUserType(refreshToken);

            if (!userType.equals(tokenUserType)) {
                log.warn("토큰 갱신 실패 - 사용자 유형 불일치: 요청={}, 토큰={}", userType, tokenUserType);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponseJson(HttpStatus.UNAUTHORIZED, Map.of(
                                "error", "사용자 유형이 일치하지 않습니다."
                        )));
            }

            // 새 AccessToken 생성
            String newAccessToken = jwtUtil.generateAccessToken(username, userType);
            log.info("새 액세스 토큰 발급 성공 - 사용자: {}", username);

            // 새 AccessToken을 쿠키에 설정
            ResponseCookie cookie = ResponseCookie.from("Authorization", newAccessToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(30 * 60 * 4) // 2시간
                    .build();

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                            "accessToken", newAccessToken,
                            "message", "액세스 토큰이 성공적으로 갱신되었습니다."
                    )));
        }

        log.warn("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰: {}", username);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseJson(HttpStatus.UNAUTHORIZED, Map.of(
                        "error", "리프레시 토큰이 유효하지 않거나 만료되었습니다."
                )));
    }


    @PostMapping("/api/logout")
    public ResponseEntity<ApiResponseJson> logout(@RequestBody RefreshTokenRequestDto request,
                                                  HttpServletResponse response) {
        String username = request.getUsername();

        log.info("로그아웃 요청 - 사용자: {}", username);

        // Redis에서 RefreshToken 삭제
        refreshTokenService.deleteRefreshToken(username);

        // 쿠키 삭제
        ResponseCookie cookie = ResponseCookie.from("Authorization", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0) // 쿠키 즉시 만료
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                        "message", "로그아웃이 성공적으로 처리되었습니다."
                )));
    }


    // ✅ 1. 네이버 OAuth 콜백 처리 및 JWT 발급
    @GetMapping("/login/oauth2/code/naver")
    public ResponseEntity<ApiResponseJson> handleNaverCallback(@RequestParam("code") String code, @RequestParam("state") String state) {
        try {
            log.info("Received code: {}", code);
            log.info("Received state: {}", state);

            // 네이버 AccessToken 요청
            String accessToken = oauthService.getAccessTokenFromNaver(code);
            log.info("네이버 액세스 토큰 : {}", accessToken);

            // 사용자 정보 요청
            Map<String, Object> userInfo = oauthService.getUserInfoFromNaver(accessToken);
            String email = (String) userInfo.get("email");
            log.info("이메일 : {}", email);
            if (email == null) {
                log.error("이메일 정보 없음");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponseJson(HttpStatus.BAD_REQUEST, Map.of("error", "이메일 정보를 가져올 수 없습니다.")));
            }

            // JWT 발급 및 리프레시 토큰 저장
            String jwt = jwtUtil.generateAccessToken(email, "CUSTOMER");
            String refreshToken = refreshTokenService.getRefreshToken(email);
            refreshTokenService.saveRefreshToken(email, "CUSTOMER", refreshToken);

            // 쿠키 설정
            ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", jwt)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(30 * 60 * 4) // 4시간
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("Refresh-Token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(30 * 60 * 24 * 7) // 7일
                    .build();

            return ResponseEntity.ok()
                    .header("Set-Cookie", accessTokenCookie.toString())
                    .header("Set-Cookie", refreshTokenCookie.toString())
                    .body(new ApiResponseJson(HttpStatus.OK, Map.of("message", "JWT가 발급되었습니다.")));
        } catch (Exception e) {
            log.error("OAuth 로그인 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseJson(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("error", "OAuth 로그인 처리 중 오류가 발생했습니다.")));
        }
    }




}
