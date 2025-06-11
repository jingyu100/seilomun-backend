package com.yju.team2.seilomun.domain.auth.service;

import com.yju.team2.seilomun.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final long REFRESH_TOKEN_EXPIRE_TIME = 14 * 24 * 60 * 60; // 14일 (초 단위)

    
    // RefreshToken 저장
    public void saveRefreshToken(String username, String userType, String refreshToken) {
        // "RT:userType:username" 형식으로 키 저장
        String key = generateKey(username, userType);
        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    // RefreshToken 교체
    public void rotateRefreshToken(String username, String userType, String newRefreshToken) {
        // 기존 토큰 삭제
        deleteRefreshToken(username, userType);
        // 새 토큰 저장
        saveRefreshToken(username, userType, newRefreshToken);
    }

    // RefreshToken 조회
    public String getRefreshToken(String username) {

        // 판매자와 소비자 모두 검색 (어느 유형인지 모르기 때문)
        String sellerKey = generateKey(username, "SELLER");
        String customerKey = generateKey(username, "CUSTOMER");

        String sellerToken = redisTemplate.opsForValue().get(sellerKey);
        if (sellerToken != null) {
            return sellerToken;
        }

        return redisTemplate.opsForValue().get(customerKey);
    }

    // 특정 유형의 RefreshToken 조회
    public String getRefreshToken(String username, String userType) {
        String key = generateKey(username, userType);
        return redisTemplate.opsForValue().get(key);
    }

    // RefreshToken 검증
    public boolean validateRefreshToken(String refreshToken, String username) {
        String storedToken = getRefreshToken(username);
        return storedToken != null && storedToken.equals(refreshToken) && jwtUtil.validateToken(refreshToken, username);
    }

    // RefreshToken 삭제 (로그아웃 시)
    public void deleteRefreshToken(String username) {

        // 두 유형 모두 삭제 시도
        String sellerKey = generateKey(username, "SELLER");
        String customerKey = generateKey(username, "CUSTOMER");

        redisTemplate.delete(sellerKey);
        redisTemplate.delete(customerKey);
    }

    // 특정 유형의 RefreshToken 삭제
    public void deleteRefreshToken(String username, String userType) {
        String key = generateKey(username, userType);
        redisTemplate.delete(key);
    }

    // Redis 키 생성 헬퍼 메서드
    private String generateKey(String username, String userType) {
        return "RT:" + userType + ":" + username;
    }
}