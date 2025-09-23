package com.yju.team2.seilomun.domain.customer.controller;

import com.yju.team2.seilomun.domain.auth.service.UserStatusService;
import com.yju.team2.seilomun.domain.customer.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
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

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        OauthAttribute oauthAttr = OauthAttribute.of(registrationId, oauth2User.getAttributes());
        String email = oauthAttr.getEmail();

        // 토큰 발급
        Map<String,String> tokens = oauthService.customerLogin(email);
        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        // 소셜 로그인시 온라인 설정
        userStatusService.updateOnlineStatus(email, "CUSTOMER");

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
