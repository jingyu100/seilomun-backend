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
public class JwtRequestFilter extends OncePerRequestFilter { // Jwt 요청 필터 클래스

    private final JwtUtil jwtUtil;
    private final JwtUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserStatusService userStatusService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, // 모든 HTTP 요청에 대해 토큰 검증 및 자동 갱신을 수행
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 액세스 토큰 추출
        String accessToken = extractTokenFromCookie(request, "access_token");
        String refreshToken = extractTokenFromCookie(request, "refresh_token");
        if (accessToken != null) {
            try {
                // 2. 액세스 토큰 검증
                if (jwtUtil.isTokenExpired(accessToken)) {
                    // 3. 토큰이 만료된 경우 자동 갱신 시도
                    handleExpiredToken(request, response, filterChain);
                    return;
                }

                // 4. 토큰이 유효한 경우 SecurityContext에 인증 정보 설정
                processValidToken(accessToken, request);

            } catch (ExpiredJwtException e) {
                // 토큰이 만료된 경우
                handleExpiredToken(request, response, filterChain);
                return;

            } catch (Exception e) {
                logger.error("JWT 처리 중 오류 발생: ", e);
            }
        } else if (refreshToken != null) {
            handleExpiredToken(request, response, filterChain);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // 만료된 토큰 처리 로직
    // 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급
    private void handleExpiredToken(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        String refreshToken = extractTokenFromCookie(request, "refresh_token");

        // refresh token이 없는 경우 - 인증 실패 처리
        if (refreshToken == null) {
            log.debug("리프레시 토큰이 없음");
            clearCookiesAndHandleAuthFailure(response);
            return;
        }

        // refresh token이 유효하지 않은 경우 - 인증 실패 처리
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            log.debug("리프레시 토큰이 유효하지 않음");
            clearCookiesAndHandleAuthFailure(response);
            return;
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            String userType = jwtUtil.extractUserType(refreshToken);

            // Redis 토큰 검증
            String storedToken = refreshTokenService.getRefreshToken(username, userType);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.warn("Redis에 저장된 토큰과 일치하지 않음 - 사용자: {}", username);
                clearCookiesAndHandleAuthFailure(response);
                return;
            }

            // 토큰 갱신 성공 - 정상 진행
            renewTokensAndProceed(request, response, filterChain, username, userType);

        } catch (Exception e) {
            log.warn("토큰 갱신 중 오류 발생: ", e);
            // redis에 존재하지 않는 리프레쉬 토큰 발견시 브라우저 쿠키 초기화
            clearCookiesAndHandleAuthFailure(response);
        }
    }

    // 쿠키 초기화 및 인증 실패 처리를 하나의 메서드로 통합
    private void clearCookiesAndHandleAuthFailure(HttpServletResponse response) throws IOException {
        // 먼저 쿠키를 초기화
        ResponseCookie expiredAccessToken = CookieUtil.createExpiredAccessTokenCookie();
        ResponseCookie expiredRefreshToken = CookieUtil.createExpiredRefreshTokenCookie();

        response.addHeader(HttpHeaders.SET_COOKIE, expiredAccessToken.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshToken.toString());

        // 그 다음 인증 실패 응답
        handleAuthenticationFailure(response);
    }

    // 토큰 갱신
    private void renewTokensAndProceed(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain filterChain,
                                       String username,
                                       String userType) throws IOException, ServletException {

        // 온라인 상태 업데이트
        userStatusService.updateOnlineStatus(username, userType);

        // 새 토큰 생성
        String newAccessToken = jwtUtil.generateAccessToken(username, userType);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userType);

        // RefreshToken 교체
        refreshTokenService.rotateRefreshToken(username, userType, newRefreshToken);

        // 쿠키 업데이트
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(newAccessToken);
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(newRefreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // SecurityContext 설정
        UserDetails userDetails = userDetailsService.loadUserByUsernameAndType(username, userType);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 정상적으로 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    // 인증 실패 응답
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

    // 유효한 토큰 처리 로직
    // 토큰에서 사용자 정보를 추출하고 Spring Security Context에 설정
    private void processValidToken(String token, HttpServletRequest request) {

        // 토큰에서 사용자 정보 추출
        String email = jwtUtil.extractUsername(token);
        String userType = jwtUtil.extractUserType(token);

        // 토큰 유효성 검증 추가
        if (email != null && userType != null &&
                jwtUtil.validateToken(token, email) && // 검증 추가
                SecurityContextHolder.getContext().getAuthentication() == null) {

            userStatusService.updateOnlineStatus(email, userType);
            UserDetails userDetails = userDetailsService.loadUserByUsernameAndType(email, userType);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // API 요청시마다 온라인 상태 갱신
        userStatusService.updateOnlineStatus(email, userType);

        // SecurityContext에 이미 인증 정보가 없는 경우에만 설정
        if (email != null && userType != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // 사용자 상세 정보 로드
            UserDetails userDetails = userDetailsService.loadUserByUsernameAndType(email, userType);

            // Spring Security 인증 토큰 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 요청 상세 정보 설정
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    // 쿠키에서 토큰 추출 헬퍼
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}