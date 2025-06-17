package com.yju.team2.seilomun.filter;

import com.yju.team2.seilomun.domain.auth.service.JwtUserDetailsService;
import com.yju.team2.seilomun.domain.auth.service.RefreshTokenService;
import com.yju.team2.seilomun.domain.auth.service.UserStatusService;
import com.yju.team2.seilomun.util.CookieUtil;
import com.yju.team2.seilomun.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserStatusService userStatusService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String accessToken = extractTokenFromCookie(request, "access_token");
        String refreshToken = extractTokenFromCookie(request, "refresh_token");

        log.debug("Access Token 존재: {}, Refresh Token 존재: {}",
                accessToken != null, refreshToken != null);

        // 1. 액세스 토큰이 있는 경우
        if (accessToken != null) {
            try {
                if (jwtUtil.isTokenExpired(accessToken)) {
                    log.debug("액세스 토큰이 만료됨, 토큰 갱신 시도");
                    if (handleTokenRefresh(request, response, filterChain)) {
                        return; // 갱신 성공시 필터 체인 종료
                    }
                } else {
                    // 유효한 액세스 토큰 처리
                    log.debug("유효한 액세스 토큰 처리");
                    processValidToken(accessToken, request);
                }
            } catch (ExpiredJwtException e) {
                log.debug("액세스 토큰 만료 예외 발생, 토큰 갱신 시도");
                if (handleTokenRefresh(request, response, filterChain)) {
                    return; // 갱신 성공시 필터 체인 종료
                }
            } catch (Exception e) {
                log.error("JWT 처리 중 오류 발생: ", e);
            }
        }
        // 2. 액세스 토큰이 없고 리프레시 토큰만 있는 경우
        else if (refreshToken != null) {
            log.debug("액세스 토큰 없음, 리프레시 토큰으로 갱신 시도");
            if (handleTokenRefresh(request, response, filterChain)) {
                return; // 갱신 성공시 필터 체인 종료
            }
        }

        // 3. 토큰 처리 후 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 갱신 처리
     * @return true: 갱신 성공하여 필터 체인 계속 진행, false: 갱신 실패하여 인증 실패 처리
     */
    private boolean handleTokenRefresh(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain filterChain) throws IOException, ServletException {

        String refreshToken = extractTokenFromCookie(request, "refresh_token");

        // 리프레시 토큰이 없는 경우
        if (refreshToken == null) {
            log.debug("리프레시 토큰이 없음");
            clearCookiesAndHandleAuthFailure(response);
            return false;
        }

        // 리프레시 토큰 유효성 검증
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            log.debug("리프레시 토큰이 유효하지 않음");
            clearCookiesAndHandleAuthFailure(response);
            return false;
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            String userType = jwtUtil.extractUserType(refreshToken);

            // Redis에서 저장된 토큰과 비교
            String storedToken = refreshTokenService.getRefreshToken(username, userType);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.warn("Redis에 저장된 토큰과 일치하지 않음 - 사용자: {}", username);
                clearCookiesAndHandleAuthFailure(response);
                return false;
            }

            // 토큰 갱신 성공
            return renewTokensAndProceed(request, response, filterChain, username, userType);

        } catch (Exception e) {
            log.warn("토큰 갱신 중 오류 발생: ", e);
            clearCookiesAndHandleAuthFailure(response);
            return false;
        }
    }

    /**
     * 토큰 갱신 및 인증 설정
     */
    private boolean renewTokensAndProceed(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain filterChain,
                                          String username,
                                          String userType) throws IOException, ServletException {

        try {
            // 1. 온라인 상태 업데이트
            userStatusService.updateOnlineStatus(username, userType);

            // 2. 새 토큰 생성
            String newAccessToken = jwtUtil.generateAccessToken(username, userType);
            String newRefreshToken = jwtUtil.generateRefreshToken(username, userType);

            // 3. RefreshToken 교체 (Redis 업데이트)
            refreshTokenService.rotateRefreshToken(username, userType, newRefreshToken);

            // 4. 쿠키 업데이트
            ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(newAccessToken);
            ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(newRefreshToken);

            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            // 5. SecurityContext 설정
            setSecurityContext(request, username, userType);

            log.debug("토큰 갱신 성공 - 사용자: {}, 타입: {}", username, userType);

            // 6. 다음 필터로 진행
            filterChain.doFilter(request, response);
            return true;

        } catch (Exception e) {
            log.error("토큰 갱신 과정에서 오류 발생: ", e);
            clearCookiesAndHandleAuthFailure(response);
            return false;
        }
    }

    /**
     * 유효한 토큰 처리
     */
    private void processValidToken(String token, HttpServletRequest request) {
        try {
            String email = jwtUtil.extractUsername(token);
            String userType = jwtUtil.extractUserType(token);

            // 토큰 유효성 검증
            if (email != null && userType != null &&
                    jwtUtil.validateToken(token, email) &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // 온라인 상태 업데이트
                userStatusService.updateOnlineStatus(email, userType);

                // SecurityContext 설정
                setSecurityContext(request, email, userType);

                log.debug("유효한 토큰 처리 완료 - 사용자: {}, 타입: {}", email, userType);
            }
        } catch (Exception e) {
            log.error("유효한 토큰 처리 중 오류 발생: ", e);
        }
    }

    /**
     * SecurityContext 설정 공통 메서드
     */
    private void setSecurityContext(HttpServletRequest request, String username, String userType) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsernameAndType(username, userType);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("SecurityContext 설정 완료 - 사용자: {}, 권한: {}",
                    username, userDetails.getAuthorities());
        } catch (Exception e) {
            log.error("SecurityContext 설정 중 오류 발생: ", e);
            throw e;
        }
    }

    /**
     * 쿠키 초기화 및 인증 실패 처리
     */
    private void clearCookiesAndHandleAuthFailure(HttpServletResponse response) throws IOException {
        // 쿠키 초기화
        ResponseCookie expiredAccessToken = CookieUtil.createExpiredAccessTokenCookie();
        ResponseCookie expiredRefreshToken = CookieUtil.createExpiredRefreshTokenCookie();

        response.addHeader(HttpHeaders.SET_COOKIE, expiredAccessToken.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshToken.toString());

        // 인증 실패 응답
        handleAuthenticationFailure(response);
    }

    /**
     * 인증 실패 응답
     */
    private void handleAuthenticationFailure(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = """
                {
                    "error": "AUTHENTICATION_FAILED",
                    "message": "인증에 실패했습니다. 다시 로그인해주세요."
                }
                """;

        response.getWriter().write(jsonResponse);
    }

    /**
     * 쿠키에서 토큰 추출
     */
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    return (value != null && !value.trim().isEmpty()) ? value : null;
                }
            }
        }
        return null;
    }
}