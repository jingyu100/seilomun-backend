package com.yju.team2.seilomun.domain.auth;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.dto.CustomerRegisterDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OauthService {

    private final CustomerRepository customerRepository;

    /**
     * 기존 회원 조회 (Optional 반환)
     */
    public Optional<Customer> findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    /**
     * 회원 등록 (기존 회원이면 에러)
     */
    public Customer registerCustomer(
            String email, String nickname, String profileImage, CustomerRegisterDto customerRegisterDto) {

        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 회원입니다.");
        }

        return customerRepository.save(Customer.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl("default_profile.png")
                .phone(customerRegisterDto.getPhone())
                .build());
    }

}
