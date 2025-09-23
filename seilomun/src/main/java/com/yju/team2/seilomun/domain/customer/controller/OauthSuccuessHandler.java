package com.yju.team2.seilomun.domain.customer.controller;

import com.yju.team2.seilomun.domain.auth.service.UserStatusService;
import com.yju.team2.seilomun.domain.customer.service.OauthService;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.yju.team2.seilomun.domain.customer.oauth.HttpCookieOAuth2AuthorizationRequestRepository.CLIENT_TYPE_PARAM_COOKIE_NAME;
import static com.yju.team2.seilomun.domain.customer.oauth.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
@RequiredArgsConstructor
@Slf4j
public class OauthSuccuessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final CustomerRepository customerRepository;
    private final UserStatusService userStatusService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    // properties에 있는 값
    @Value("${app.oauth.redirectUrl}")
    private String redirectUrl;

    //  Oauth Login 후 핸들러
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Oauth 로그인 성공 !!");

        Optional<String> clientTypeCookie = CookieUtil.getCookie(request, CLIENT_TYPE_PARAM_COOKIE_NAME).map(c -> c.getValue());
        Optional<String> redirectUriCookie = CookieUtil.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME).map(c -> c.getValue());

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
        log.info("생일 : {}",oauthAttr.getBirthday());

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
                    oauthAttr.getBirthday().replace("-",""),
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
        // 소셜 로그인시 온라인 설정
        userStatusService.updateOnlineStatus(oauthAttr.getEmail(), "CUSTOMER");

        // Clear authentication cookies
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        if (clientTypeCookie.isPresent() && "app".equalsIgnoreCase(clientTypeCookie.get())) {
            // For App Client: Respond with HTML to post message
            response.setContentType("text/html;charset=UTF-8");
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head><title>Login Success</title></head>
                <body>
                    <script>
                        try {
                            const tokens = { accessToken: '%s', refreshToken: '%s' };
                            window.ReactNativeWebView.postMessage(JSON.stringify(tokens));
                        } catch (e) {
                            console.error('Failed to post message to React Native WebView', e);
                        }
                    </script>
                    <p>Processing login...</p>
                </body>
                </html>
                """, accessToken, refreshToken);

            response.getWriter().write(html);
            response.getWriter().flush();
        } else {
            // For Web Client: Set cookies and redirect
            String targetUrl = redirectUriCookie.orElse(redirectUrl);

            ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(accessToken);
            ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(refreshToken);

            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            log.info("로그인 완료, 리다이렉트: {}", targetUrl);
            response.sendRedirect(targetUrl);
        }
    }

    // Oauth 제공자, 속성 (Naver: response , kakao : profile,account)
    private OauthAttribute extractOauthAttribute(String provider, Map<String, Object> attributes) {
        return OauthAttribute.of(provider,attributes);
    }

}
