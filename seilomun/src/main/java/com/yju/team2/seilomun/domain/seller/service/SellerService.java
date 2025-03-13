package com.yju.team2.seilomun.domain.seller.service;

import com.yju.team2.seilomun.domain.seller.entity.DeliveryFee;
import com.yju.team2.seilomun.domain.seller.repository.DeliveryFeeRepository;
import com.yju.team2.seilomun.domain.seller.repository.SellerPhotoRepository;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.dto.DeliveryFeeDto;
import com.yju.team2.seilomun.dto.SellerInformationDto;
import com.yju.team2.seilomun.dto.SellerLoginDto;
import com.yju.team2.seilomun.dto.SellerRegisterDto;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerService {
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private final SellerRepository sellerRepository;
    private final SellerPhotoRepository sellerPhotoRepository;
    private final DeliveryFeeRepository deliveryFeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    // 판매자 가입
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

    //판매자 로그인
    public String sellerLogin(SellerLoginDto sellerLoginDto){
        Optional<Seller> byEmail = sellerRepository.findByEmail(sellerLoginDto.getEmail());
        if (byEmail.isEmpty()){
            throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
        }
        Seller seller = byEmail.get();
        if (!passwordEncoder.matches(sellerLoginDto.getPassword(), seller.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치 하지 않습니다.");
        }
        return jwtUtil.generateToken(seller.getEmail());
    }
    
    // 비밀번호 정규식 검사
    private void checkPasswordStrength(String password) {
        if (PASSWORD_PATTERN.matcher(password).matches()) {
            return;
        }
        log.info("비밀번호 정책 미달");
        throw new IllegalArgumentException("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.");
    }

    public Seller updateSellerInformation(String email, SellerInformationDto sellerInformationDto) {
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        seller.updateInformation(sellerInformationDto);

        log.info("판매자 매장 정보가 성공적으로 업데이트되었습니다: {}", seller.getEmail());
        return sellerRepository.save(seller);
    }

}
