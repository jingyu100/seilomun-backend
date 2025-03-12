package com.yju.team2.seilomun.domain.customer.service;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.dto.CustomerRegisterDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRegisterDto validCustomerDto;
    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        // 유효한 회원가입 DTO 설정
        validCustomerDto = new CustomerRegisterDto();
        validCustomerDto.setEmail("test@example.com");
        validCustomerDto.setPassword("Test123!");
        validCustomerDto.setName("홍길동");
        validCustomerDto.setPhone("010-1234-5678");
        validCustomerDto.setNickname("길동이");
        validCustomerDto.setBirthdate(String.valueOf(LocalDate.of(1990, 1, 1)));
        validCustomerDto.setGender('M');

        // 저장된 Customer 객체 설정
        savedCustomer = Customer.builder()
                .email(validCustomerDto.getEmail())
                .password("encodedPassword")
                .name(validCustomerDto.getName())
                .phone(validCustomerDto.getPhone())
                .nickname(validCustomerDto.getNickname())
                .birthDate(validCustomerDto.getBirthdate())
                .gender(validCustomerDto.getGender())
                .profileImageUrl("default.png")
                .points(0)
                .status('0')
                .deletedAt(null)
                .build();
    }

    @Test
    @DisplayName("유효한 회원가입 정보로 회원가입 성공")
    void registerCustomer_WithValidInfo_ShouldSucceed() {
        // Given
        given(customerRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(customerRepository.save(any(Customer.class))).willReturn(savedCustomer);

        // When
        Customer result = customerService.registerCustomer(validCustomerDto);

        // Then
        assertNotNull(result);
        assertEquals(validCustomerDto.getEmail(), result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(validCustomerDto.getName(), result.getName());
        assertEquals(validCustomerDto.getPhone(), result.getPhone());
        assertEquals(validCustomerDto.getNickname(), result.getNickname());

        verify(customerRepository).existsByEmail(validCustomerDto.getEmail());
        verify(passwordEncoder).encode(validCustomerDto.getPassword());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 예외 발생")
    void registerCustomer_WithExistingEmail_ShouldThrowException() {
        // Given
        given(customerRepository.existsByEmail(anyString())).willReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(validCustomerDto)
        );

        assertEquals("이미 등록된 이메일입니다.", exception.getMessage());
        verify(customerRepository).existsByEmail(validCustomerDto.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("비밀번호가 정책을 만족하지 않을 때 예외 발생 - 특수문자 누락")
    void registerCustomer_WithWeakPassword_NoSpecialChar_ShouldThrowException() {
        // Given
        validCustomerDto.setPassword("Test1234"); // 특수문자 없음

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(validCustomerDto)
        );

        assertEquals("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.", exception.getMessage());
        verify(customerRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("비밀번호가 정책을 만족하지 않을 때 예외 발생 - 숫자 누락")
    void registerCustomer_WithWeakPassword_NoDigit_ShouldThrowException() {
        // Given
        validCustomerDto.setPassword("TestPass!"); // 숫자 없음

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(validCustomerDto)
        );

        assertEquals("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.", exception.getMessage());
        verify(customerRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("비밀번호가 정책을 만족하지 않을 때 예외 발생 - 영문자 누락")
    void registerCustomer_WithWeakPassword_NoLetter_ShouldThrowException() {
        // Given
        validCustomerDto.setPassword("12345678!"); // 영문자 없음

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(validCustomerDto)
        );

        assertEquals("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("비밀번호가 정책을 만족하지 않을 때 예외 발생 - 8자 미만")
    void registerCustomer_WithWeakPassword_TooShort_ShouldThrowException() {
        // Given
        validCustomerDto.setPassword("Aa1!"); // 8자 미만

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(validCustomerDto)
        );

        assertEquals("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.", exception.getMessage());
    }
}