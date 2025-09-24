package com.yju.team2.seilomun.domain.customer.service;

import com.yju.team2.seilomun.domain.auth.service.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.oauth.OauthAttribute;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient; // WebClient import 추가


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OauthService {

    private final CustomerRepository customerRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    // WebClient를 사용하기 위해 의존성 주입 추가
    private final WebClient.Builder webClientBuilder;


    // [기존 코드] DB에 email이 있다면 token 발급/저장 (변경 없음)
    public Map<String,String> customerLogin(String email) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);

        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 이메일 입니다");
        }

        Customer customer = optionalCustomer.get();

        String refreshToken = jwtUtil.generateRefreshToken(customer.getEmail(),"CUSTOMER");
        refreshTokenService.saveRefreshToken(customer.getEmail(),"CUSTOMER",refreshToken);

        String accessToken = jwtUtil.generateAccessToken(customer.getEmail(),"CUSTOMER");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken",accessToken);
        tokens.put("refreshToken",refreshToken);

        return tokens;
    }

    // [기존 코드] DB에 회원 저장 (변경 없음)
    public Customer registerCustomer(
            String name, String birthday, String email, String nickname, String profileImage) {
        log.info("이메일 : email: {}, nickname: {}, profileImage: {}", email, nickname, profileImage);
        Customer customer = Customer.builder()
                .email(email)
                .password("oauth_user") // 소셜 로그인 사용자는 별도 비밀번호 없음
                .nickname(nickname)
                .profileImageUrl(profileImage)
                .name(name)
                .phone("01000000000") // 소셜 로그인 시 임시 값, 추가 정보 입력 필요
                .birthDate(birthday)
                .gender('U') // 소셜 로그인 시 임시 값, 추가 정보 입력 필요
                .points(0)
                .status('0')
                .type('U')
                .deletedAt(null)
                .build();
        log.info("신규 회원 저장 - email: {}, nickname: {}, profileImage: {}",
                customer.getEmail(), customer.getNickname(), customer.getProfileImageUrl());

        return customerRepository.save(customer);
    };

    // --- 👇 [새로 추가된 코드] ---

    /**
     * 앱에서 받은 카카오 accessToken으로 카카오 서버에 사용자 정보를 요청하는 메소드
     * @param accessToken 앱에서 받은 카카오 토큰
     * @return 카카오 서버가 보내준 사용자 정보(Map 형태)
     */
    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        // WebClient: 다른 서버의 API를 호출할 때 사용하는 도구
        WebClient webClient = webClientBuilder.baseUrl("https://kapi.kakao.com") // 카카오 서버 주소
                .defaultHeader("Authorization", "Bearer " + accessToken) // 헤더에 인증 토큰 추가
                .build();

        // 카카오 서버의 /v2/user/me 라는 주소로 GET 요청을 보냄
        return webClient.get()
                .uri("/v2/user/me")
                .retrieve() // 응답을 받음
                .bodyToMono(Map.class) // 응답 내용을 Map 형태로 변환
                .block(); // 비동기 처리가 끝날 때까지 기다림
    }

    /**
     * 앱을 위한 카카오 로그인 전체 과정을 처리하는 총괄 메소드
     * @param kakaoAccessToken 앱이 보내준 카카오 토큰
     * @return 우리 서비스의 토큰과 사용자 정보
     */
    public Map<String, Object> kakaoLogin(String kakaoAccessToken) {
        // 1. 카카오 서버로부터 사용자 정보 받아오기
        Map<String, Object> userInfo = getKakaoUserInfo(kakaoAccessToken);
        log.info("카카오 사용자 정보: {}", userInfo);

        // 2. 받아온 정보를 OauthAttribute로 변환 (기존 코드 재사용!)
        OauthAttribute oauthAttr = OauthAttribute.of("kakao", userInfo);

        // 3. DB에 이미 가입된 사용자인지 확인
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(oauthAttr.getEmail());

        Customer customer;
        if (optionalCustomer.isEmpty()) {
            // 4-1. 신규 사용자인 경우, 회원가입 처리 (기존 코드 재사용!)
            log.info("신규 카카오 사용자입니다. 자동 회원가입을 진행합니다.");
            customer = registerCustomer(
                    oauthAttr.getName(),
                    oauthAttr.getBirthday() != null ? oauthAttr.getBirthday().replace("-", "") : "",
                    oauthAttr.getEmail(),
                    oauthAttr.getNickname(),
                    oauthAttr.getProfile()
            );
        } else {
            // 4-2. 기존 사용자인 경우, 정보를 가져옴
            log.info("기존 카카오 사용자입니다. 로그인을 진행합니다.");
            customer = optionalCustomer.get();
        }

        // 5. 우리 서비스의 JWT 토큰 발급 (기존 코드 재사용!)
        Map<String, String> tokens = customerLogin(customer.getEmail());

        // 6. 앱에 전달할 최종 결과(JSON)를 구성
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", tokens.get("accessToken"));
        result.put("refreshToken", tokens.get("refreshToken"));
        // 앱에서 사용하기 편하도록 user 객체 안에 사용자 정보를 담아서 전달
        result.put("user", Map.of(
            "id", customer.getId(),
            "email", customer.getEmail(),
            "nickname", customer.getNickname(),
            "userType", "CUSTOMER"
        ));

        return result;
    }
}

