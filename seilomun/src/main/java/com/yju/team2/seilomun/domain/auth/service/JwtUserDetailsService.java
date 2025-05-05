package com.yju.team2.seilomun.domain.auth.service;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService {

    private final SellerRepository sellerRepository;
    private final CustomerRepository customerRepository;

    // userType에 따라 적절한 사용자 정보 로드
    public UserDetails loadUserByUsernameAndType(String email, String userType) {
        if ("CUSTOMER".equals(userType)) {
            return loadCustomerByUsername(email);
        } else if ("SELLER".equals(userType)) {
            return loadSellerByUsername(email);
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 유형: " + userType);
        }
    }

    // 소비자 정보 로드
    public UserDetails loadCustomerByUsername(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("소비자를 찾을 수 없습니다: " + email));

        return JwtUserDetails.fromCustomer(
                customer.getId(),
                customer.getEmail(),
                customer.getName()
        );
    }

    // 판매자 정보 로드
    public UserDetails loadSellerByUsername(String email) {
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("판매자를 찾을 수 없습니다: " + email));

        return JwtUserDetails.fromSeller(
                seller.getId(),
                seller.getEmail(),
                seller.getStoreName()
        );
    }

}
