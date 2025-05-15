package com.yju.team2.seilomun.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    // AccessToken 유효시간: 2시간
    private final long ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000 * 4;

    // RefreshToken 유효시간: 14일
    private final long REFRESH_TOKEN_EXPIRE_TIME = 14 * 24 * 60 * 60 * 1000;

    // AccessToken 생성
    public String generateAccessToken(String username, String userType) {
        return generateToken(username, userType, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // RefreshToken 생성
    public String generateRefreshToken(String username, String userType) {
        return generateToken(username, userType, REFRESH_TOKEN_EXPIRE_TIME);
    }

    // 토큰 생성
    private String generateToken(String username, String userType, long expireTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", userType); // "SELLER" 또는 "CUSTOMER"

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // 토큰에서 사용자 정보 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 토큰에서 userType 추출
    public String extractUserType(String token) {
        return extractClaim(token, claims -> claims.get("userType", String.class));
    }

    // 토큰에서 유효시간 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 클레임 추출
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    // 토큰 유효성 검사
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // 토큰 만료 여부만 확인 (예외 던지지 않음)
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("토큰 만료 확인 중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token format");
        }
    }

    // 리프레시 토큰 검증
    public boolean validateRefreshToken(String token) {
        try {
            String username = extractUsername(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}

