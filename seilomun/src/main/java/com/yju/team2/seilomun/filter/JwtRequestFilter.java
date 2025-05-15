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

        // 1. 리프레시 토큰 추출
        String refreshToken = extractTokenFromCookie(request, "refresh_token");

        if (refreshToken != null && jwtUtil.validateRefreshToken(refreshToken)) {
            try {
                // 2. 리프레시 토큰에서 사용자 정보 추출
                String username = jwtUtil.extractUsername(refreshToken);
                String userType = jwtUtil.extractUserType(refreshToken);

                // Redis에 저장된 토큰과 비교
                String storedToken = refreshTokenService.getRefreshToken(username, userType);
                if (storedToken == null || !storedToken.equals(refreshToken)) {
                    log.error("Redis에 저장된 토큰과 일치하지 않음");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 온라인 상태 업데이트
                userStatusService.updateOnlineStatus(username, userType);

                // 3. 새 토큰 생성
                String newAccessToken = jwtUtil.generateAccessToken(username, userType);
                String newRefreshToken = jwtUtil.generateRefreshToken(username, userType);

                // 4. RefreshToken 교체
                refreshTokenService.rotateRefreshToken(username, userType, newRefreshToken);

                // 5. 쿠키 업데이트
                ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(newAccessToken);
                ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(newRefreshToken);

                response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

                // 6. SecurityContext 설정
                UserDetails userDetails = userDetailsService.loadUserByUsernameAndType(username, userType);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                log.info("토큰 자동 갱신 실패: ", e);
                // 갱신 실패 시 인증 없이 계속 진행
            }
        }

        // 리프레시 토큰이 없거나 유효하지 않은 경우
        filterChain.doFilter(request, response);
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