package com.yju.team2.seilomun.domain.seller.service;

import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.dto.SellerRegisterDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerService {
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    public Seller sellerRegister(SellerRegisterDto sellerRegisterDto){
        checkPasswordStrength(sellerRegisterDto.getPassword());

        if (sellerRepository.existsByEmail(sellerRegisterDto.getEmail())){
            log.info("이미 존재하는 이메일입니다.");
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        Seller seller = Seller.builder()
                .email(sellerRegisterDto.getEmail())
                .password(passwordEncoder.encode(sellerRegisterDto.getPassword()))
                .businessNumber(sellerRegisterDto.getBusinessNumber())
                .storeName(sellerRegisterDto.getStoreName())
                .category(sellerRegisterDto.getCategory())
                .addressDetail(sellerRegisterDto.getAddressDetail())
                .phone(sellerRegisterDto.getPhone())
                //여기서부턴 임시
                .status('1')
                .postCode("11111")
                .operatingHours("12")
                .deliveryAvailable('0')
                .rating(0F)
                .pickupTime("30분")
                .isOpen('0')
                .build();
        return sellerRepository.save(seller);
    }


    private void checkPasswordStrength(String password) {
        if (PASSWORD_PATTERN.matcher(password).matches()) {
            return;
        }
        log.info("비밀번호 정책 미달");
        throw new IllegalArgumentException("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.");
    }

}
