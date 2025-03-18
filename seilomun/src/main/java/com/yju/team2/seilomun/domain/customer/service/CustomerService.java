package com.yju.team2.seilomun.domain.customer.service;

import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.dto.CustomerLoginDto;
import com.yju.team2.seilomun.dto.CustomerRegisterDto;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public Customer registerCustomer(CustomerRegisterDto customerRegisterDto) {
        checkPasswordStrength(customerRegisterDto.getPassword());

        if (customerRepository.existsByEmail(customerRegisterDto.getEmail())) {
            log.info("이미 존재하는 이메일입니다.");
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        Customer customer = Customer.builder()
                .email(customerRegisterDto.getEmail())
                .password(passwordEncoder.encode(customerRegisterDto.getPassword()))
                .name(customerRegisterDto.getName())
                .phone(customerRegisterDto.getPhone())
                .nickname(customerRegisterDto.getNickname())
                .birthDate(customerRegisterDto.getBirthdate())
                .gender(customerRegisterDto.getGender())
                .profileImageUrl("default.png")
                .points(0)
                .status('0')
                .deletedAt(null)
                .build();
        
        return customerRepository.save(customer);
    }

    private void checkPasswordStrength(String password) {
        if (PASSWORD_PATTERN.matcher(password).matches()) {
            return;
        }
        log.info("비밀번호 정책 미달");
        throw new IllegalArgumentException("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.");
    }

    //소비자 로그인
    public String customerLogin(CustomerLoginDto customerLoginDto) {
        Optional<Customer> byEmail = customerRepository.findByEmail(customerLoginDto.getEmail());
        if (byEmail.isEmpty()) {
            log.info(byEmail.toString());
            throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
        }
        Customer customer = byEmail.get();
        if (!passwordEncoder.matches(customerLoginDto.getPassword(), customer.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치 하지 않습니다.");
        }

        // RefreshToken 생성 및 Redis에 저장
        String refreshToken = jwtUtil.generateRefreshToken(customer.getEmail(), "CUSTOMER");
        refreshTokenService.saveRefreshToken(customer.getEmail(), "CUSTOMER", refreshToken);

        // AccessToken 생성 및 반환
        return jwtUtil.generateAccessToken(customer.getEmail(), "CUSTOMER");
    }
}
