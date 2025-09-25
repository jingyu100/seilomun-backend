package com.yju.team2.seilomun.domain.auth.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.dto.SocialLoginRequestDto;
import com.yju.team2.seilomun.domain.auth.service.SocialAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    public SocialAuthController(SocialAuthService socialAuthService) {
        this.socialAuthService = socialAuthService;
    }

    @PostMapping("/social-login")
    public ResponseEntity<ApiResponseJson> socialLogin(@RequestBody @Valid SocialLoginRequestDto req) {
        // 1) 파라미터 1차 검증
        if (req.isCodeProvider()) {
            if (!StringUtils.hasText(req.getCode()) || !StringUtils.hasText(req.getRedirectUri())) {
                return error(HttpStatus.BAD_REQUEST, "code/redirectUri가 필요합니다. (KAKAO/NAVER)");
            }
        } else if (req.isIdTokenProvider()) {
            if (!StringUtils.hasText(req.getIdToken())) {
                return error(HttpStatus.BAD_REQUEST, "idToken이 필요합니다. (GOOGLE/APPLE)");
            }
        } else {
            return error(HttpStatus.BAD_REQUEST, "지원하지 않는 provider 입니다. (KAKAO|NAVER|GOOGLE|APPLE)");
        }

        // 2) 원본 속성 조회
        Map<String, Object> raw = socialAuthService.fetchUserAttributes(req);

        // 3) 정제 사용자 정보
        Map<String, Object> user = socialAuthService.toSanitizedUser(req.getProvider(), raw);

        // 4) 회원 upsert + JWT 발급 (기존 로직 재사용)
        Map<String, Object> issued = socialAuthService.upsertAndIssueTokens(req.getProvider(), raw, user);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "소셜 로그인 성공");
        data.put("provider", req.getProvider().toUpperCase());
        data.put("user", issued.get("user"));
        data.put("accessToken", issued.get("accessToken"));
        data.put("refreshToken", issued.get("refreshToken"));

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, data));
    }

    private ResponseEntity<ApiResponseJson> error(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(new ApiResponseJson(status, Map.of("error", msg)));
    }
}
