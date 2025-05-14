package com.yju.team2.seilomun.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.auth.dto.*;
import com.yju.team2.seilomun.domain.auth.service.*;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.common.ApiResponseJson;
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

    private static final String TOKEN_MISMATCH_ERROR = "사용자 유형이 일치하지 않습니다.";
    private static final String INVALID_TOKEN_ERROR = "리프레시 토큰이 유효하지 않거나 만료되었습니다.";
    private static final String TOKEN_REFRESH_SUCCESS = "액세스 토큰이 성공적으로 갱신되었습니다.";
    private static final String LOGOUT_SUCCESS = "로그아웃이 성공적으로 처리되었습니다.";

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

        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        userStatusService.updateOnlineStatus(loginRequest.getEmail(), loginRequest.getUserType());

        // 쿠키 설정
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(accessToken);
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                        "message", "로그인 성공",
                        "userType", loginRequest.getUserType()
                )));
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

    // 오류 응답 생성
    private ResponseEntity<ApiResponseJson> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponseJson(status, Map.of("error", message)));
    }
}