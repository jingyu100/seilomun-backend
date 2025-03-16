package com.yju.team2.seilomun.filter;

import com.yju.team2.seilomun.domain.auth.JwtUserDetailsService;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor()
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);
        if (token != null) {
            try {
                String email = jwtUtil.extractUsername(token);
                String userType = jwtUtil.extractUserType(token);

                if (email != null && userType != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    if (jwtUtil.validateToken(token, email)) {
                        // userType에 따라 적절한 UserDetails 로드
                        UserDetails userDetails = userDetailsService.loadUserByUsernameAndType(email, userType);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                logger.error("JWT 토큰 처리 중 오류 발생: ", e);
                // 여기서 오류를 처리 안해도 됨
                // 인증 실패시 SecurityContext에 인증 객체가 설정되지 않고, 보안 필터에서 처리
            }
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Authorization".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
