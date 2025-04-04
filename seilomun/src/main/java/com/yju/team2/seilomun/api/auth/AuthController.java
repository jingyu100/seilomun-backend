package com.yju.team2.seilomun.api.auth;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.RefreshTokenRequestDto;
import com.yju.team2.seilomun.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    // RefreshToken을 사용하여 새로운 AccessToken을 발급
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseJson> refreshToken(@RequestBody RefreshTokenRequestDto request) {

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
                    .maxAge(60 * 60 * 2) // 2시간
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


    @PostMapping("/logout")
    public ResponseEntity<ApiResponseJson> logout(@RequestBody RefreshTokenRequestDto request) {
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
}
