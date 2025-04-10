package com.yju.team2.seilomun.api.customer;

import com.yju.team2.seilomun.domain.auth.OauthService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.oauth.OauthAttribute;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    private final CustomerRepository customerRepository;

    // properties에 있는 값
    @Value("${app.oauth.redirectUrl}")
    private String redirectUrl;

    //  Oauth Login 후 핸들러
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Oauth 로그인 성공 !!");

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        log.info("사용자 정보 : {}",oauth2User.getAttributes());
        // Oauth 제공자 naver, kakao 인지 끝에 적혀 있기 때문에

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
        log.info("닉네임 : {} ", oauthAttr.getNickname());

        // 프로필 이미지
        log.info("프로필 이미지 : {}", oauthAttr.getProfile());


        // 회원 조회
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(oauthAttr.getEmail());

        if(optionalCustomer.isPresent()){
            log.info("기존 로그인 : {}", optionalCustomer.get().getEmail());
        }else {
            Customer customer = oauthService.registerCustomer(
                    oauthAttr.getName(),
                    oauthAttr.getBirthday(),
                    oauthAttr.getEmail(),
                    oauthAttr.getNickname(),
                    oauthAttr.getProfile()
            );
            log.info("신규 회원 : {}",customer.getEmail());


        }

        // 토큰 쿠키에 저장
        Map<String,String> tokens = oauthService.customerLogin(oauthAttr.getEmail());

        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        // 액세스 토큰용 쿠키 설정 (2시간 만료)
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(accessToken);

        // 리프레시 토큰용 쿠키 설정 (14일 만료)
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // 로그인 성공 후 리다이렉트
        log.info("로그인 완료: {}", oauthAttr.getEmail());
        response.sendRedirect(redirectUrl);


    }

    // Oauth 제공자, 속성 (Naver: response , kakao : profile,account)
    private OauthAttribute extractOauthAttribute(String provider, Map<String, Object> attributes) {
        return OauthAttribute.of(provider,attributes);
    }

}
