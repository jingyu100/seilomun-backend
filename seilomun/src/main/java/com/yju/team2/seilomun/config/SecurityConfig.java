package com.yju.team2.seilomun.config;

import com.yju.team2.seilomun.domain.customer.controller.OauthSuccuessHandler;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final OauthSuccuessHandler oauthSuccuessHandler;

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
                        // 소비자만 접근 가능
                        .requestMatchers(new AntPathRequestMatcher("/api/customers/orders/**")).hasRole("CUSTOMER")
                        .requestMatchers(new AntPathRequestMatcher("/customer/**")).hasRole("CUSTOMER")
                        .requestMatchers(new AntPathRequestMatcher("/api/customers/wishes/**")).hasRole("CUSTOMER")
                        .requestMatchers(new AntPathRequestMatcher("/api/customers/favorites/**")).hasRole("CUSTOMER")
                        // 알림 관련 엔드포인트 - 인증된 사용자만 접근 허용
                        .requestMatchers("/api/notifications/**").authenticated()
                        // 판매자만 접근 가능
                        .requestMatchers(new AntPathRequestMatcher("/seller/**")).hasRole("SELLER")
                        // 공통 API (둘 다 접근 가능)
                        .requestMatchers(new AntPathRequestMatcher("/api/common/**")).hasAnyRole("SELLER", "CUSTOMER")
                        .requestMatchers(
                                "/api/auth/businessVerification",
                                "/api/auth/login",
                                "/api/sellers/**",
                                "/api/customers/**",
                                "/api/auth/logout",
                                "/api/address/**",
                                "/h2-console/**",
                                "/error",
                                "/favicon.ico",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/products/**",
                                "/ws/**",        // WebSocket 엔드포인트 허용
                                "/ws/info/**",   // SockJS 정보 경로 허용
                                "/ws/websocket/**",
                                "/login/**", // 인가코드 처리
                                "/oauth2/authorization/**",//네이버로그인
                                "/login/oauth2/code/**",
                                "/api/auth/email",
                                "/api/auth/verifyEmail",
                                "/api/orders/buy",
                                "/api/orders/test/buy",
                                "/api/orders/toss/success",  // Toss 결제 성공 콜백
                                "/api/orders/toss/fail",  // 정적 리소스로 사용할 경우
                                "/api/chat/**",
                                "/api/users/**",
                                "/api/search/autocomplete",
                                "/api/search/fuzzy",
                                "/api/search/popular",
                                "/api/products/search",
                                "/api/sellers/search"
                        ).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauthSuccuessHandler) // 로그인 성공 후 이동
                        .failureUrl("http://localhost:5173")    //로그인 실패 후 이동
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
