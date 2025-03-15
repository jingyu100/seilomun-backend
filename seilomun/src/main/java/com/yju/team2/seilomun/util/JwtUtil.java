package com.yju.team2.seilomun.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    // AccessToken 유효시간: 2시간
    private final long ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000 * 4;

    // RefreshToken 유효시간: 14일
    private final long REFRESH_TOKEN_EXPIRE_TIME = 14 * 24 * 60 * 60 * 1000;

    // AccessToken 생성
    public String generateAccessToken(String username) {
        return generateToken(username, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // RefreshToken 생성
    public String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_TOKEN_EXPIRE_TIME);
    }

    // 토큰 생성
    private String generateToken(String username, long expireTime) {
        Map<String, Object> claims = new HashMap<>();
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

    // 토큰에서 유효시간 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

//    // 토큰에서 Claims (주장) 추출
//    private Claims extractClaims(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(secretKey)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }

    // 클레임 추출
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    // 토큰 만료 여부 확인
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 토큰 유효성 검사
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}

