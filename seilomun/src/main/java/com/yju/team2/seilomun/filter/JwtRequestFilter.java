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
public class JwtRequestFilter extends OncePerRequestFilter {

    
    private final JwtUtil jwtUtil;
    private final JwtUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserStatusService userStatusService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.debug("요청 경로: {}", requestPath);

        String accessToken = extractTokenFromCookie(request, "access_token");
        if (accessToken == null) {
            accessToken = extractTokenFromAuthorizationHeader(request);
        }
        String refreshToken = extractTokenFromCookie(request, "refresh_token");

        log.debug("Access Token 존재: {}, Refresh Token 존재: {}",
                accessToken != null, refreshToken != null);

        try {
            // 1. Access Token이 있고 유효한 경우
            if (accessToken != null && isValidAccessToken(accessToken)) {
                log.debug("유효한 Access Token으로 인증 처리");
                authenticateWithToken(accessToken, request);
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Access Token이 없거나 만료된 경우 - Refresh Token으로 갱신 시도
            if (refreshToken != null) {
                String newAccessToken = tryRefreshTokens(request, response, refreshToken);
                if (newAccessToken != null) {
                    log.debug("토큰 갱신 성공, 새 Access Token으로 인증 처리");
                    authenticateWithToken(newAccessToken, request);
                    filterChain.doFilter(request, response);
                    return;
                }
                // 갱신 실패했지만 여기서 쿠키를 삭제하지 않음
                log.debug("토큰 갱신 실패 - 인증되지 않은 상태로 진행");
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
     * Access Token 전용 유효성 검사
     */
    private boolean isValidAccessToken(String token) {
        try {
            return !jwtUtil.isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Access Token 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 유효한 토큰으로 인증 처리
     */
    private void authenticateWithToken(String token, HttpServletRequest request) {
        try {
            String username = extractUsernameFromToken(token);
            String userType = extractUserTypeFromToken(token);

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
     * Refresh Token으로 토큰 갱신 시도 (새 Access Token 반환)
     */
    private String tryRefreshTokens(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        try {
            // 먼저 Refresh Token에서 사용자 정보 추출 (만료되어도 추출 가능)
            String username = extractUsernameFromToken(refreshToken);
            String userType = extractUserTypeFromToken(refreshToken);

            if (username == null || userType == null) {
                log.debug("Refresh Token에서 사용자 정보 추출 실패");
                return null;
            }

            // Redis에서 저장된 토큰과 비교
            String storedToken = refreshTokenService.getRefreshToken(username, userType);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.warn("Redis에 저장된 토큰과 일치하지 않음 - 사용자: {}", username);
                // 여기서도 쿠키를 삭제하지 않고 갱신만 실패 처리
                return null;
            }

            // Refresh Token 자체의 유효성 검사 (만료 확인)
            if (jwtUtil.isTokenExpired(refreshToken)) {
                log.debug("Refresh Token이 만료됨 - 사용자: {}", username);
                // 만료된 경우에만 쿠키 삭제
                clearAuthenticationCookies(response);
                refreshTokenService.deleteRefreshToken(username, userType);
                return null;
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

            log.debug("토큰 갱신 성공 - 사용자: {}, 타입: {}", username, userType);

            return newAccessToken;

        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: ", e);
            // 여기서도 쿠키를 삭제하지 않음
            return null;
        }
    }

    /**
     * 만료된 토큰에서도 안전하게 사용자명 추출
     */
    private String extractUsernameFromToken(String token) {
        try {
            return jwtUtil.extractUsername(token);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 사용자명은 추출 가능
            return e.getClaims().getSubject();
        } catch (Exception e) {
            log.error("토큰에서 사용자명 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서도 안전하게 사용자 타입 추출
     */
    private String extractUserTypeFromToken(String token) {
        try {
            return jwtUtil.extractUserType(token);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 사용자 타입은 추출 가능
            return e.getClaims().get("userType", String.class);
        } catch (Exception e) {
            log.error("토큰에서 사용자 타입 추출 실패: {}", e.getMessage());
            return null;
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
     * 인증 쿠키 초기화 (진짜 만료된 경우에만 사용)
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

    private String extractTokenFromAuthorizationHeader(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
        return header.substring(7);
    }
    return null;
}
}