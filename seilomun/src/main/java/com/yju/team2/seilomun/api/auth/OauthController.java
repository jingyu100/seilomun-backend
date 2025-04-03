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

@RequestMapping("/oauth")
@Controller
@Slf4j
@RequiredArgsConstructor
public class OauthController {

    private final JwtUtil jwtUtil;
    private final OauthService oauthService;

    /**
     * 회원가입 (OAuth 로그인 후 추가 정보 입력)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponseJson> registerCustomer(@RequestBody CustomerRegisterDto customerRegisterDto) {
        log.info("회원 등록 요청 : {} ", customerRegisterDto.getEmail());

        Customer customer = oauthService.registerCustomer(
                customerRegisterDto.getEmail(),
                customerRegisterDto.getNickname(),
                "default_profile.png",
                customerRegisterDto
        );

        String accessToken = jwtUtil.generateAccessToken(customer.getEmail(), "CUSTOMER");
        String refreshToken = jwtUtil.generateRefreshToken(customer.getEmail(), "CUSTOMER");

        log.info("회원가입 완료 - JWT 발급 : {} ", customer.getEmail());

        return ResponseEntity.ok(
                new ApiResponseJson(
                        HttpStatus.OK,
                        Map.of("accessToken", accessToken,
                                "refreshToken", refreshToken,
                                "message", "회원가입이 완료되었습니다.")));
    }

    /**
     * 로그인 (이메일로 회원 찾기)
     */
    @GetMapping("/login")
    public ResponseEntity<ApiResponseJson> login(@RequestParam String email) {
        log.info("기존 회원 로그인 요청 : {} ", email);

        Optional<Customer> customerOptional = oauthService.findCustomerByEmail(email);

        if (customerOptional.isEmpty()) {
            log.warn("로그인 실패 - 가입되지 않은 이메일: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseJson(HttpStatus.NOT_FOUND,
                            Map.of("message", "가입되지 않은 이메일입니다.")));
        }

        Customer customer = customerOptional.get();
        String accessToken = jwtUtil.generateAccessToken(email, "CUSTOMER");
        String refreshToken = jwtUtil.generateRefreshToken(email, "CUSTOMER");

        return ResponseEntity.ok(
                new ApiResponseJson(
                        HttpStatus.OK,
                        Map.of("accessToken", accessToken,
                                "refreshToken", refreshToken,
                                "message", "로그인이 완료되었습니다.")));
    }
}
