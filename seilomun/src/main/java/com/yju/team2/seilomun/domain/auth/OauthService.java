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


    public Customer registerCustomer(
            String name, String birthday, String email, String nickname, String profileImage) {
        log.info("이메일 : email: {}, nickname: {}, profileImage: {}", email, nickname, profileImage);
        return customerRepository.findByEmail(email).orElseGet(() ->  {

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
        });
    }

}
