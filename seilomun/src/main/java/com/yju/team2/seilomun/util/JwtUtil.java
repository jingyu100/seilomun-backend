package com.yju.team2.seilomun.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

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

    // AccessToken 유효시간: 30분
    private final long ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000;

    // RefreshToken 유효시간: 14일
    private final long REFRESH_TOKEN_EXPIRE_TIME = 14 * 24 * 60 * 60 * 1000L;

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
        claims.put("userType", userType);
        claims.put("tokenType", expireTime == ACCESS_TOKEN_EXPIRE_TIME ? "ACCESS" : "REFRESH");

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
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 사용자명은 추출 가능
            return e.getClaims().getSubject();
        }
    }

    // 토큰에서 userType 추출
    public String extractUserType(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userType", String.class));
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 userType은 추출 가능
            return e.getClaims().get("userType", String.class);
        }
    }

    // 토큰에서 유효시간 추출
    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        }
    }

    // 클레임 추출
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            throw e; // 만료 예외는 그대로 전파
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.error("JWT 파싱 오류: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    // 토큰 유효성 검사 (사용자명과 만료시간 확인)
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            boolean expired = expiration.before(new Date());
            log.debug("토큰 만료 확인 - 만료 시간: {}, 현재 시간: {}, 만료됨: {}",
                    expiration, new Date(), expired);
            return expired;
        } catch (ExpiredJwtException e) {
            log.debug("토큰이 만료됨: {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("토큰 만료 확인 중 오류: {}", e.getMessage());
            return true; // 오류 발생시 만료된 것으로 처리
        }
    }

    // 리프레시 토큰 검증
    public boolean validateRefreshToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.debug("Refresh Token이 null이거나 빈 문자열");
                return false;
            }

            String username = extractUsername(token);
            String userType = extractUserType(token);

            if (username == null || userType == null) {
                log.debug("Refresh Token에서 사용자 정보 추출 실패");
                return false;
            }

            boolean isExpired = isTokenExpired(token);
            log.debug("Refresh Token 검증 - 사용자: {}, 타입: {}, 만료됨: {}",
                    username, userType, isExpired);

            return !isExpired;
        } catch (Exception e) {
            log.error("RefreshToken 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 토큰 타입 확인 (ACCESS/REFRESH)
    public String getTokenType(String token) {
        try {
            return extractClaim(token, claims -> claims.get("tokenType", String.class));
        } catch (ExpiredJwtException e) {
            return e.getClaims().get("tokenType", String.class);
        } catch (Exception e) {
            log.error("토큰 타입 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    // 토큰 남은 시간 계산 (분 단위)
    public long getRemainingTimeInMinutes(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining / (60 * 1000)); // 분 단위로 변환
        } catch (Exception e) {
            return 0;
        }
    }
}