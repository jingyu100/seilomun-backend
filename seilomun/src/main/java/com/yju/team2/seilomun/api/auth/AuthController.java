package com.yju.team2.seilomun.api.auth;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.RefreshTokenRequestDto;
import com.yju.team2.seilomun.util.CookieUtil;
import com.yju.team2.seilomun.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
@Tag(name = "인증 관리", description = "토큰 갱신 및 인증 관련 API")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    private static final String TOKEN_MISMATCH_ERROR = "사용자 유형이 일치하지 않습니다.";
    private static final String INVALID_TOKEN_ERROR = "리프레시 토큰이 유효하지 않거나 만료되었습니다.";
    private static final String TOKEN_REFRESH_SUCCESS = "액세스 토큰이 성공적으로 갱신되었습니다.";
    private static final String LOGOUT_SUCCESS = "로그아웃이 성공적으로 처리되었습니다.";

    // RefreshToken을 사용하여 새로운 AccessToken을 발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseJson> refreshToken(@RequestBody RefreshTokenRequestDto request) {
        String username = request.getUsername();
        String userType = request.getUserType();

        log.info("토큰 갱신 요청 - 사용자: {}, 유형: {}", username, userType);

        try {
            // Redis에서 저장된 RefreshToken 조회
            String refreshToken = refreshTokenService.getRefreshToken(username);
            if (refreshToken == null) {
                log.warn("토큰 갱신 실패 - 저장된 리프레시 토큰 없음: {}", username);
                return createErrorResponse(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_ERROR);
            }

            // RefreshToken 유효성 검증
            if (!refreshTokenService.validateRefreshToken(refreshToken, username)) {
                log.warn("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰: {}", username);
                return createErrorResponse(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_ERROR);
            }

            // 토큰에서 userType 검증
            String tokenUserType = jwtUtil.extractUserType(refreshToken);
            if (!userType.equals(tokenUserType)) {
                log.warn("토큰 갱신 실패 - 사용자 유형 불일치: 요청={}, 토큰={}", userType, tokenUserType);
                return createErrorResponse(HttpStatus.UNAUTHORIZED, TOKEN_MISMATCH_ERROR);
            }

            // 새 AccessToken 생성
            String newAccessToken = jwtUtil.generateAccessToken(username, userType);
            log.info("새 액세스 토큰 발급 성공 - 사용자: {}", username);

            // 새 RefreshToken 생성 (Token Rotation)
            String newRefreshToken = jwtUtil.generateRefreshToken(username, userType);

            // 새 RefreshToken을 Redis에 저장하고 이전 토큰 무효화
            refreshTokenService.rotateRefreshToken(username, userType, newRefreshToken);

            // 쿠키 설정 및 응답 생성
            ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(newAccessToken);
            ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(newRefreshToken);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                            "accessToken", newAccessToken,
                            "refreshToken", newRefreshToken,
                            "message", TOKEN_REFRESH_SUCCESS
                    )));
        } catch (Exception e) {
            log.error("토큰 갱신 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");
        }
    }

    // 사용자 로그아웃을 처리
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseJson> logout(@RequestBody RefreshTokenRequestDto request) {
        String username = request.getUsername();
        log.info("로그아웃 요청 - 사용자: {}", username);

        try {
            // Redis에서 RefreshToken 삭제
            refreshTokenService.deleteRefreshToken(username);

            // 만료된 쿠키 생성
            ResponseCookie accessTokenCookie = CookieUtil.createExpiredAccessTokenCookie();
            ResponseCookie refreshTokenCookie = CookieUtil.createExpiredRefreshTokenCookie();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(new ApiResponseJson(HttpStatus.OK, Map.of("message", LOGOUT_SUCCESS)));
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "로그아웃 처리 중 오류가 발생했습니다");
        }
    }

    @PostMapping("/ping")
    public ResponseEntity<ApiResponseJson> ping(@AuthenticationPrincipal JwtUserDetails user) {
        if (user != null) {
            log.debug("인증 확인 - 사용자: {}", user.getUsername());
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                    "authenticated", true,
                    "username", user.getUsername(),
                    "userType", user.getUserType()
            )));
        }
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다");
    }

    // 오류 응답 생성
    private ResponseEntity<ApiResponseJson> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponseJson(status, Map.of("error", message)));
    }
}