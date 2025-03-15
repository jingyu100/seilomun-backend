package com.yju.team2.seilomun.domain.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final long REFRESH_TOKEN_EXPIRE_TIME = 14 * 24 * 60 * 60; // 14일 (초 단위)

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // RefreshToken 저장
    public void saveRefreshToken(String username, String refreshToken) {
        // "RT:username" 형식으로 키 저장
        String key = "RT:" + username;
        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    // RefreshToken 조회
    public String getRefreshToken(String username) {
        String key = "RT:" + username;
        return redisTemplate.opsForValue().get(key);
    }

    // RefreshToken 검증
    public boolean validateRefreshToken(String username, String refreshToken) {
        String storedToken = getRefreshToken(username);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    // RefreshToken 삭제 (로그아웃 시)
    public void deleteRefreshToken(String username) {
        String key = "RT:" + username;
        redisTemplate.delete(key);
    }
}