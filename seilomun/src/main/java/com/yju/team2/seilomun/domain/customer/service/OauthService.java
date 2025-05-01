package com.yju.team2.seilomun.domain.customer.service;

import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    
    // DB에 email이 있다면 token 발급/저장
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

    // DB에 회원 저장
    public Customer registerCustomer(
            String name, String birthday, String email, String nickname, String profileImage) {
        log.info("이메일 : email: {}, nickname: {}, profileImage: {}", email, nickname, profileImage);
            Customer customer = Customer.builder()
                    .email(email)
                    .password("oauth_user")
                    .nickname(nickname)
                    .profileImageUrl(profileImage)
                    .name(name)
                    .phone("01011111111")
                    .birthDate(birthday)
                    .gender('U')
                    .points(0)
                    .status('0')
                    .deletedAt(null)
                    .build();
            log.info("신규 회원 저장 - email: {}, nickname: {}, profileImage: {}",
                    customer.getEmail(), customer.getNickname(), customer.getProfileImageUrl());

            return customerRepository.save(customer);
        };
}

