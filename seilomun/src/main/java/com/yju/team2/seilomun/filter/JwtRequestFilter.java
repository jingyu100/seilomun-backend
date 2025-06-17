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
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter { // Jwt 요청 필터 클래스

    private final JwtUtil jwtUtil;
    private final JwtUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserStatusService userStatusService;

    // 토큰 검증이 필요없는 경로들
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/customers/signup",
            "/api/sellers/signup",
            "/api/auth/logout",
            "/h2-console",
            "/swagger-ui",
            "/v3/api-docs",
            "/ws",
            "/login",
            "/oauth2",
            "/api/auth/email",
            "/api/auth/verifyEmail"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.debug("요청 경로: {}", requestPath);

        // 토큰 검증이 필요없는 경로는 바로 통과
        if (shouldSkipTokenValidation(requestPath)) {
            log.debug("토큰 검증 제외 경로: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = extractTokenFromCookie(request, "access_token");
        String refreshToken = extractTokenFromCookie(request, "refresh_token");

        log.debug("Access Token 존재: {}, Refresh Token 존재: {}",
                accessToken != null, refreshToken != null);

        try {
            // 1. Access Token이 유효한 경우
            if (accessToken != null && isValidToken(accessToken)) {
                log.debug("유효한 Access Token으로 인증 처리");
                authenticateWithToken(accessToken, request);
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Access Token이 만료되었거나 없는 경우 - Refresh Token으로 갱신 시도
            if (refreshToken != null && tryRefreshTokens(request, response, refreshToken)) {
                log.debug("토큰 갱신 성공, 요청 계속 처리");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 토큰이 없거나 모두 유효하지 않은 경우
            log.debug("유효한 토큰이 없음 - 인증되지 않은 상태로 진행");
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT 필터 처리 중 오류 발생: ", e);
            // 오류 발생시에도 필터 체인 계속 진행 (Spring Security가 처리)
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 토큰 검증을 건너뛸 경로인지 확인
     */
    private boolean shouldSkipTokenValidation(String requestPath) {
        return EXCLUDED_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 토큰 유효성 검사
     */
    private boolean isValidToken(String token) {
        try {
            return !jwtUtil.isTokenExpired(token);
        } catch (Exception e) {
            log.debug("토큰 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 유효한 토큰으로 인증 처리
     */
    private void authenticateWithToken(String token, HttpServletRequest request) {
        try {
            String username = jwtUtil.extractUsername(token);
            String userType = jwtUtil.extractUserType(token);

            if (username != null && userType != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // 온라인 상태 업데이트
                userStatusService.updateOnlineStatus(username, userType);

                // SecurityContext 설정
                setSecurityContext(request, username, userType);

                log.debug("인증 성공 - 사용자: {}, 타입: {}", username, userType);
            }
        } catch (Exception e) {
            log.error("토큰 인증 처리 중 오류: ", e);
        }
    }

    /**
     * Refresh Token으로 토큰 갱신 시도
     */
    private boolean tryRefreshTokens(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        try {
            // Refresh Token 유효성 검증
            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                log.debug("Refresh Token이 유효하지 않음");
                clearAuthenticationCookies(response);
                return false;
            }

            String username = jwtUtil.extractUsername(refreshToken);
            String userType = jwtUtil.extractUserType(refreshToken);

            // Redis에서 저장된 토큰과 비교
            String storedToken = refreshTokenService.getRefreshToken(username, userType);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.warn("Redis에 저장된 토큰과 일치하지 않음 - 사용자: {}", username);
                clearAuthenticationCookies(response);
                return false;
            }

            // 새 토큰 생성
            String newAccessToken = jwtUtil.generateAccessToken(username, userType);
            String newRefreshToken = jwtUtil.generateRefreshToken(username, userType);

            // Redis 업데이트
            refreshTokenService.rotateRefreshToken(username, userType, newRefreshToken);

            // 새 쿠키 설정
            setNewTokenCookies(response, newAccessToken, newRefreshToken);

            // 온라인 상태 업데이트
            userStatusService.updateOnlineStatus(username, userType);

            // SecurityContext 설정
            setSecurityContext(request, username, userType);

            log.debug("토큰 갱신 성공 - 사용자: {}, 타입: {}", username, userType);
            return true;

        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: ", e);
            clearAuthenticationCookies(response);
            return false;
        }
    }

    /**
     * SecurityContext 설정
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
            log.error("SecurityContext 설정 중 오류: ", e);
            throw e;
        }
    }

    /**
     * 새로운 토큰 쿠키 설정
     */
    private void setNewTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(accessToken);
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    /**
     * 인증 쿠키 초기화
     */
    private void clearAuthenticationCookies(HttpServletResponse response) {
        ResponseCookie expiredAccessToken = CookieUtil.createExpiredAccessTokenCookie();
        ResponseCookie expiredRefreshToken = CookieUtil.createExpiredRefreshTokenCookie();

        response.addHeader(HttpHeaders.SET_COOKIE, expiredAccessToken.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshToken.toString());
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