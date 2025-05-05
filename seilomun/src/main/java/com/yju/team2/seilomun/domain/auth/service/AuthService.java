package com.yju.team2.seilomun.domain.auth.service;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public Map<String, String> login(String email, String password, String userType) {
        // 사용자 유형에 따라 로그인 처리
        if ("CUSTOMER".equals(userType)) {
            return loginCustomer(email, password);
        } else if ("SELLER".equals(userType)) {
            return loginSeller(email, password);
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 유형입니다.");
        }
    }

    private Map<String, String> loginCustomer(String email, String password) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);

        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
        }

        Customer customer = optionalCustomer.get();

        if (!passwordEncoder.matches(password, customer.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return generateTokens(email, "CUSTOMER");
    }

    private Map<String, String> loginSeller(String email, String password) {
        Optional<Seller> optionalSeller = sellerRepository.findByEmail(email);

        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
        }

        Seller seller = optionalSeller.get();

        if (!passwordEncoder.matches(password, seller.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return generateTokens(email, "SELLER");
    }

    private Map<String, String> generateTokens(String email, String userType) {
        // RefreshToken 생성 및 Redis에 저장
        String refreshToken = jwtUtil.generateRefreshToken(email, userType);
        refreshTokenService.saveRefreshToken(email, userType, refreshToken);

        // AccessToken 생성
        String accessToken = jwtUtil.generateAccessToken(email, userType);

        // 두 토큰을 맵에 담아 반환
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }
}
