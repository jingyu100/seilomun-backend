package com.yju.team2.seilomun.api.auth;

import com.yju.team2.seilomun.dto.UserLoginRequestDto;
import com.yju.team2.seilomun.util.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "사용자 관리", description = "사용자 관리 API")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/api/auth/login")
    public String login(@RequestBody UserLoginRequestDto request) {
        // 여기서 사용자 인증을 처리하고
        // 예를 들어, 사용자 이름과 비밀번호를 확인하고 인증된 사용자라면 JWT를 반환
        return jwtUtil.generateToken(request.getUsername());
    }
}
