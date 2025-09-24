package com.yju.team2.seilomun.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.auth.dto.*;
import com.yju.team2.seilomun.domain.auth.service.*;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.customer.dto.KakaoLoginRequestDto; // <-- 카카오 DTO import 추가
import com.yju.team2.seilomun.domain.customer.service.OauthService;     // <-- OauthService import 추가
import com.yju.team2.seilomun.util.CookieUtil;
import com.yju.team2.seilomun.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
@Tag(name = "인증 관리", description = "토큰 갱신 및 인증 관련 API")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final MailService mailService;
    private final BusinessVerificationService businessVerificationService;
    private final UserStatusService userStatusService;
    private final OauthService oauthService; // OauthService를 사용할 수 있도록 추가

    private static final String TOKEN_MISMATCH_ERROR = "사용자 유형이 일치하지 않습니다.";
    private static final String INVALID_TOKEN_ERROR = "리프레시 토큰이 유효하지 않거나 만료되었습니다.";
    private static final String TOKEN_REFRESH_SUCCESS = "액세스 토큰이 성공적으로 갱신되었습니다.";
    private static final String LOGOUT_SUCCESS = "로그아웃이 성공적으로 처리되었습니다.";
    private static final String REFRESH_SUCCESS = "토큰이 성공적으로 갱신되었습니다.";

    // 통합 로그인 메서드
    @PostMapping("/login")
    public ResponseEntity<ApiResponseJson> login(@Valid @RequestBody LoginRequestDto loginRequest,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        Map<String, String> tokens = authService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                loginRequest.getUserType()
        );

        userStatusService.updateOnlineStatus(loginRequest.getEmail(), loginRequest.getUserType());

        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        // 쿠키 설정
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(accessToken);
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                        "message", "로그인 성공",
                        "userType", loginRequest.getUserType(),
                        "accessToken", accessToken,
                        "refreshToken", refreshToken
                )));
    }

    // Refresh Token으로 Access Token 재발급 (RN/모바일 클라이언트용)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseJson> refresh(@RequestBody RefreshTokenRequestDto requestDto) {
        try {
            String refreshToken = requestDto.getRefreshToken();

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "refreshToken이 필요합니다");
            }

            String username = jwtUtil.extractUsername(refreshToken);
            String userType = jwtUtil.extractUserType(refreshToken);

            if (username == null || userType == null) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다");
            }

            String storedToken = refreshTokenService.getRefreshToken(username, userType);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다");
            }

            if (jwtUtil.isTokenExpired(refreshToken)) {
                // 만료: 서버/클라이언트 모두 정리 유도
                refreshTokenService.deleteRefreshToken(username, userType);
                return createErrorResponse(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다");
            }

            String newAccessToken = jwtUtil.generateAccessToken(username, userType);
            String newRefreshToken = jwtUtil.generateRefreshToken(username, userType);

            refreshTokenService.rotateRefreshToken(username, userType, newRefreshToken);

            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                    "message", REFRESH_SUCCESS,
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken,
                    "userType", userType,
                    "username", username
            )));
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.UNAUTHORIZED, "토큰 갱신에 실패했습니다");
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

            userStatusService.removeOnlineStatus(username);
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

    @PostMapping("/email")
    public ResponseEntity<ApiResponseJson> sendVerificationEmail(@Valid @RequestBody EmailVerificationRequestDto requestDto,
                                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    bindingResult.getFieldError().getDefaultMessage()
            );
        }

        try {
            // 사용자 이메일로 인증 메일 발송
            mailService.sendAuthMail(requestDto.getEmail());

            return ResponseEntity.ok()
                    .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                            "message", "인증 메일이 발송되었습니다",
                            "email", requestDto.getEmail()
                    )));
        } catch (Exception e) {
            log.error("이메일 인증 코드 발송 중 오류 발생: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송 중 오류가 발생했습니다");
        }
    }

    @PostMapping("/verifyEmail")
    public ResponseEntity<ApiResponseJson> verifyEmail(@Valid @RequestBody EmailVerificationCodeDto requestDto,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    bindingResult.getFieldError().getDefaultMessage()
            );
        }

        boolean isVerified = mailService.verifyEmailAuth(
                requestDto.getEmail(),
                requestDto.getAuthNumber());

        if (isVerified) {
            return ResponseEntity.ok()
                    .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                            "message", "이메일 인증이 완료되었습니다",
                            "verified", true
                    )));
        } else {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않거나 만료되었습니다");
        }
    }

    @PostMapping("/businessVerification")
    public ResponseEntity<ApiResponseJson> verifyBusiness(@RequestBody BusinessVerificationRequestDto requestDto) {
        // 서비스를 통해 사업자 검증 수행
        Map<String, Object> resultMap = businessVerificationService.verifyBusiness(requestDto);

        // 응답 상태 코드 추출
        HttpStatus status = (HttpStatus) resultMap.remove("status");

        // 에러가 없는 경우
        return ResponseEntity.status(status)
                .body(new ApiResponseJson(status, resultMap));
    }

    /**
     * React Native 앱으로부터 카카오 로그인 요청을 받는 API 엔드포인트
     * @param requestDto 앱이 보내준 카카오 accessToken이 담긴 객체
     * @return 성공 시 우리 서비스의 토큰과 사용자 정보, 실패 시 에러 메시지
     */
    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponseJson> kakaoLogin(@RequestBody KakaoLoginRequestDto requestDto) {
        try {
            // 1. OauthService에게 앱이 보내준 카카오 토큰을 넘겨주며 모든 로그인 절차를 위임합니다.
            Map<String, Object> result = oauthService.kakaoLogin(requestDto.getAccessToken());
            
            // 2. OauthService가 성공적으로 일을 마치면, 그 결과를 ApiResponseJson으로 감싸서
            //    HTTP 상태 코드 200(OK)와 함께 앱에게 응답합니다.
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, result));
        } catch (Exception e) {
            // 3. 만약 OauthService가 일을 처리하다가 중간에 예외(에러)를 발생시키면,
            //    createErrorResponse 헬퍼 메소드를 사용하여 일관된 에러 응답을 보냅니다.
            log.error("카카오 로그인 처리 중 오류: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했습니다.");
        }
    }


    // 오류 응답 생성 (기존 코드)
    private ResponseEntity<ApiResponseJson> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponseJson(status, Map.of("error", message)));
    }
}
