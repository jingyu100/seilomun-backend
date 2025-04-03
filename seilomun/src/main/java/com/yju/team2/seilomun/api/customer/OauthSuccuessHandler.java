package com.yju.team2.seilomun.api.customer;

import com.yju.team2.seilomun.domain.auth.OauthService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.oauth.OauthAttribute;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OauthSuccuessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Oauth 로그인 성공 !!");

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        log.info("사용자 정보 : {}",oauth2User.getAttributes());
        // Oauth 제공자 naver, kakao 인지 끝에 적혀 있기 때문에

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        log.info("Oauth 제공자 : {}",registrationId);

        // Oauth 사용자 정보 처리
        OauthAttribute userInfo = OauthAttribute.of(registrationId, oauth2User.getAttributes());
        log.info("userInfo: {}", userInfo);
        String email = userInfo.getEmail();
        String nickName = userInfo.getNickname();
        String profileImage = userInfo.getProfile();

        //기존 회원 확인
        Optional<Customer> isExistingUser = oauthService.findCustomerByEmail(email);

        if (isExistingUser != null) {
            String accessToken = jwtUtil.generateAccessToken(email, "CUSTOMER");
            String refreshToken = jwtUtil.generateRefreshToken(email, "CUSTOMER");

            String redirectUrl = "http://localhost:5173/oauth-success?accessToken=" + accessToken + "&refreshToken=" + refreshToken;
            log.info("기존 회원 로그인 완료: {}", email);
            response.sendRedirect(redirectUrl);
        } else {
            String redirectUrl = "http://localhost:5173/signup?email=" + email + "&nickName=" + nickName + "&profile=" + profileImage;
            log.info("신규 회원, 추가 정보 입력 필요: {}", email);
            response.sendRedirect(redirectUrl);
        }
    }
}
