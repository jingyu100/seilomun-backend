package com.yju.team2.seilomun.config;

import com.yju.team2.seilomun.filter.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보안 해제
                .cors(Customizer.withDefaults()) // CORS 설정 추가
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()) // 같은 출처(도메인)에서는 iframe 허용
                )
                // 세션 관리: JWT를 사용하므로 STATELESS 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/seller/login",
                                "/api/customer/login",
                                "/api/sellers",
                                "/api/customers",
                                "/refresh-token",
                                "/logout",
                                "/h2-console/**",
                                "/error",
                                "/favicon.ico",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // 판매자만 접근 가능
                        .requestMatchers(new AntPathRequestMatcher("/seller/**")).hasRole("SELLER")
                        // 소비자만 접근 가능
                        .requestMatchers(new AntPathRequestMatcher("/customer/**")).hasRole("CUSTOMER")
                        // 공통 API (둘 다 접근 가능)
                        .requestMatchers(new AntPathRequestMatcher("/api/common/**")).hasAnyRole("SELLER", "CUSTOMER")
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable) // 기본 로그인 폼 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 인증 비활성화
                // 인증/인가 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 인증되지 않은 사용자의 보호된 리소스 접근 시 401
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Authentication required.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 권한이 없는 사용자의 접근 시 403
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access denied.\"}");
                        })
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
