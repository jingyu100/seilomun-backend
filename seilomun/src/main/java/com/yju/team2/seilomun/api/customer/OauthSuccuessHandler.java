package com.yju.team2.seilomun.api.customer;

import com.yju.team2.seilomun.domain.auth.OauthService;
import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.oauth.OauthAttribute;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    private final RefreshTokenService refreshTokenService;

    //  카카오
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

        refreshTokenService.saveRefreshToken(oauthAttr.getEmail(), "CUSTOMER", refreshToken);

        ResponseCookie cookie = ResponseCookie.from("Authorization", accessToken)
                .httpOnly(true)
                .secure(false) // 개발환경에서는 false, 운영에서는 true
                .sameSite("Lax")
                .path("/")
                .maxAge(60 * 60 * 2) // 2시간
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 로그인 성공 후 리다이렉트
        String redirectUrl = "http://localhost:5173/oauth-success?accessToken=" + accessToken + "&refreshToken=" + refreshToken;
        log.info("로그인 완료: {}", oauthAttr.getEmail());
        response.sendRedirect(redirectUrl);


    }


    private OauthAttribute extractOauthAttribute(String provider, Map<String, Object> attributes) {
        return OauthAttribute.of(provider,attributes);
    }

    // 쿠키에 토큰 삽입
    private static ResponseCookie getResponseCookie(String token) {
        // HttpOnly & Secure 쿠키 설정
        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .httpOnly(true)  // JavaScript에서 접근 불가능
                .secure(false)    // HTTPS에서만 전송
                .sameSite("None") // CSRF 방지를 위한 SameSite 설정
                /*
                Strict = 다른 사이트에서 요청할 때 쿠키가 전송되지 않음
                Lax = GET 요청 같은 안전한 요청에서는 쿠키 전송 가능
                None = 크로스 사이트 요청에서도 쿠키 사용 가능(HTTPS 필수)
                * */
                .path("/")       // 모든 경로에서 쿠키 사용 가능
                .maxAge(30 * 60 * 4) // 2시간 유지
                .build();
        return cookie;
    }
}
