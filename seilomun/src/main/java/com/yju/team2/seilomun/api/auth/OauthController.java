package com.yju.team2.seilomun.api.auth;

import com.yju.team2.seilomun.domain.auth.OauthService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.CustomerRegisterDto;
import com.yju.team2.seilomun.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RequestMapping("api/oauth")
@Controller
@Slf4j
@RequiredArgsConstructor
public class OauthController {

    private final JwtUtil jwtUtil;
    private final OauthService oauthService;

    // 로그인 및 자동 회원가입
    @GetMapping("/login")
    public ResponseEntity<ApiResponseJson> loginOrRegister(@RequestParam String name, @RequestParam String birthday,
                                                            @RequestParam String email,
                                                           @RequestParam(required = false) String nickname,
                                                           @RequestParam(required = false) String profileImage) {
        log.info("OAuth 로그인 요청 : {} ", email);

        // 회원이 없으면 자동 가입
        Customer customer = oauthService.findCustomerByEmail(email)
                .orElseGet(() -> {
                    log.info("신규 회원 자동 가입: {}", email);
                    return oauthService.registerCustomer(name, birthday,email, nickname, profileImage);
                });

        // JWT 발급
        String accessToken = jwtUtil.generateAccessToken(customer.getEmail(), "CUSTOMER");
        String refreshToken = jwtUtil.generateRefreshToken(customer.getEmail(), "CUSTOMER");

        return ResponseEntity.ok(
                new ApiResponseJson(
                        HttpStatus.OK,
                        Map.of("accessToken", accessToken,
                                "refreshToken", refreshToken,
                                "message", "로그인이 완료되었습니다.")));
    }

    // 마이페이지 수정 메서드 구현
}
