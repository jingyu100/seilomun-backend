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
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OauthSuccuessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final JwtUtil jwtUtil;
    
    
    //  카카오
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Oauth 로그인 성공 !!");

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        log.info("사용자 정보 : {}", oauth2User.getAttributes());

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        log.info("Oauth 제공자 : {}", registrationId);

        // Oauth 사용자 정보 처리
        Map<String,Object> attributes = oauth2User.getAttributes();
        log.info("사용자 속성 : {}", attributes);

        // OAuth 사용자 정보 추출
        OauthAttribute oauthAttr = extractOauthAttribute(registrationId, oauth2User.getAttributes());

        //이름
        log.info("이름 : {}", oauthAttr.getName());

        //생일
        log.info("생일 : {}",oauthAttr.getBirthday().split("-"));

        // 이메일
        log.info("이메일 : {} ", oauthAttr.getEmail());

        // 닉네임
        log.info(" 닉네임 : {} ", oauthAttr.getNickname());

        // 프로필 이미지
        log.info("프로필 이미지 : {}", oauthAttr.getProfile());


        Optional<Customer> isExistingUser = oauthService.findCustomerByEmail(oauthAttr.getEmail());

        // 기존 회원이 없으면 자동 회원가입
        Customer customer = isExistingUser.orElseGet(() -> {
            log.info("신규 회원 자동 가입: {}", oauthAttr.getEmail());

            String birthdayMMYY = oauthAttr.getBirthday().replace("-","");

            return oauthService.registerCustomer(oauthAttr.getName(),birthdayMMYY,oauthAttr.getEmail(),oauthAttr.getNickname(),oauthAttr.getProfile());
        });

        // JWT 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(customer.getEmail(), "CUSTOMER");
        String refreshToken = jwtUtil.generateRefreshToken(customer.getEmail(), "CUSTOMER");

        // 로그인 성공 후 리다이렉트
        String redirectUrl = "http://localhost:5173/oauth-success?accessToken=" + accessToken + "&refreshToken=" + refreshToken;
        log.info("로그인 완료: {}", oauthAttr.getEmail());
        response.sendRedirect(redirectUrl);
    }


    private OauthAttribute extractOauthAttribute(String provider, Map<String, Object> attributes) {
        return OauthAttribute.of(provider,attributes);
    }

}
