package com.yju.team2.seilomun.domain.customer.service;

import com.yju.team2.seilomun.domain.auth.service.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OauthService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final CustomerRepository customerRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // DefaultOAuth2UserService를 사용하여 OAuth2 공급자로부터 사용자 정보를 가져옵니다.
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        // OAuth2 공급자(예: kakao, naver)를 식별합니다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 사용자 이름 속성의 키를 가져옵니다. (예: kakao는 "id", naver는 "response")
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 사용자 속성을 OauthAttribute DTO로 변환합니다.
        OauthAttribute attributes = OauthAttribute.of(registrationId, oauth2User.getAttributes());

        // 이메일을 기반으로 사용자를 찾거나 새로 등록합니다.
        Customer customer = saveOrUpdate(attributes);

        // Spring Security 컨텍스트에서 사용될 OAuth2User 객체를 반환합니다.
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                attributes.getAttributes(),
                userNameAttributeName);
    }

    private Customer saveOrUpdate(OauthAttribute attributes) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(attributes.getEmail());

        Customer customer;
        if (optionalCustomer.isPresent()) {
            // 기존 사용자인 경우, 정보를 업데이트할 수 있습니다 (선택 사항).
            customer = optionalCustomer.get();
            log.info("기존 로그인: {}", customer.getEmail());
        } else {
            // 신규 사용자인 경우, 회원가입 처리합니다.
            customer = registerCustomer(
                    attributes.getName(),
                    attributes.getBirthday().replace("-",""),
                    attributes.getEmail(),
                    attributes.getNickname(),
                    attributes.getProfile()
            );
            log.info("신규 회원: {}", customer.getEmail());
        }
        return customer;
    }


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
                    .type('U')
                    .deletedAt(null)
                    .build();
            log.info("신규 회원 저장 - email: {}, nickname: {}, profileImage: {}",
                    customer.getEmail(), customer.getNickname(), customer.getProfileImageUrl());

            return customerRepository.save(customer);
        };
}

