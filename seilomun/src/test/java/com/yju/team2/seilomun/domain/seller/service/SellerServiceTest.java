package com.yju.team2.seilomun.domain.seller.service;

import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.dto.SellerLoginDto;
import com.yju.team2.seilomun.domain.seller.dto.SellerRegisterDto;
import com.yju.team2.seilomun.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private SellerService sellerService;

    private SellerRegisterDto sellerRegisterDto;
    private SellerLoginDto sellerLoginDto;
    private Seller seller;

    @BeforeEach
    void setUp() {
//        sellerRegisterDto = new SellerRegisterDto("test@seller.com", "12345", "Test123!", "MyStore", "Category", "01012345678", "01012345678");
        sellerLoginDto = new SellerLoginDto("test@seller.com", "Test123!");

        seller = Seller.builder()
                .email(sellerRegisterDto.getEmail())
                .password("encodedPassword")
                .businessNumber(sellerRegisterDto.getBusinessNumber())
                .storeName(sellerRegisterDto.getStoreName())
//                .category(sellerRegisterDto.getCategory())
                .addressDetail(sellerRegisterDto.getAddressDetail())
                .phone(sellerRegisterDto.getPhone())
                .status('1')
                .postCode("11111")
                .operatingHours("12")
                .deliveryAvailable('0')
                .rating(0F)
                .pickupTime("30분")
                .isOpen('0')
                .build();
    }

    @Test
    @DisplayName("판매자 회원가입 성공")
    void registerSeller_WithValidInfo_ShouldSucceed() {
        given(sellerRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(sellerRepository.save(any(Seller.class))).willReturn(seller);

        Seller result = sellerService.sellerRegister(sellerRegisterDto);

        assertNotNull(result);
        assertEquals(sellerRegisterDto.getEmail(), result.getEmail());
        verify(sellerRepository).save(any(Seller.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 예외 발생")
    void registerSeller_WithExistingEmail_ShouldThrowException() {
        given(sellerRepository.existsByEmail(anyString())).willReturn(true);

        assertThrows(IllegalArgumentException.class, () -> sellerService.sellerRegister(sellerRegisterDto));
        verify(sellerRepository, never()).save(any(Seller.class));
    }

    @Test
    @DisplayName("비밀번호 정책 미달 시 예외 발생")
    void registerSeller_WithWeakPassword_ShouldThrowException() {
        sellerRegisterDto.setPassword("weakpass");

        assertThrows(IllegalArgumentException.class, () -> sellerService.sellerRegister(sellerRegisterDto));
    }

    @Test
    @DisplayName("판매자 로그인 성공")
    void loginSeller_WithValidCredentials_ShouldReturnToken() {
        given(sellerRepository.findByEmail(anyString())).willReturn(Optional.of(seller));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
//        given(jwtUtil.generateToken(anyString())).willReturn("mockToken");

//        String token = sellerService.sellerLogin(sellerLoginDto);

//        assertNotNull(token);
//        assertEquals("mockToken", token);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
    void loginSeller_WithInvalidEmail_ShouldThrowException() {
        given(sellerRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> sellerService.sellerLogin(sellerLoginDto));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
    void loginSeller_WithInvalidPassword_ShouldThrowException() {
        given(sellerRepository.findByEmail(anyString())).willReturn(Optional.of(seller));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThrows(IllegalArgumentException.class, () -> sellerService.sellerLogin(sellerLoginDto));
    }
}


