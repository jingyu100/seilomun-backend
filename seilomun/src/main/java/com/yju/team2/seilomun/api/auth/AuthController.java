package com.yju.team2.seilomun.api.auth;

import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.AuthenticationResponse;
import com.yju.team2.seilomun.dto.UserLoginRequestDto;
import com.yju.team2.seilomun.dto.UsernameRequest;
import com.yju.team2.seilomun.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "사용자 관리", description = "사용자 관리 API")
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

//    @PostMapping("/api/auth/login")
//    public String login(@RequestBody UserLoginRequestDto request) {
//        // 여기서 사용자 인증을 처리하고
//        // 예를 들어, 사용자 이름과 비밀번호를 확인하고 인증된 사용자라면 JWT를 반환
//        return jwtUtil.generateToken(request.getUsername());
//    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseJson> refreshToken(@RequestBody UsernameRequest request, HttpServletResponse response) {

        String username = request.getUsername();

        // Redis에서 저장된 RefreshToken 조회
        String refreshToken = refreshTokenService.getRefreshToken(username);

        System.out.println("refreshToken : " + refreshToken);
        if (refreshToken != null && jwtUtil.validateToken(refreshToken, username)) {

            // 새 AccessToken 생성
            String newAccessToken = jwtUtil.generateAccessToken(username);

            // 새 AccessToken을 쿠키에 설정
            Cookie accessTokenCookie = new Cookie("Authentication", newAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(30 * 60 * 4); // 30분
            response.addCookie(accessTokenCookie);

            return ResponseEntity.ok()
                    .body(new ApiResponseJson(HttpStatus.OK, newAccessToken));
        }

//        return ResponseEntity.badRequest().body("Invalid refresh token");
        return ResponseEntity.ok()
                .body(new ApiResponseJson(HttpStatus.BAD_REQUEST, Map.of(
                        "Invalid refresh token", "인증되지 않은 리프레쉬 토큰입니다"
                )));
    }
}
