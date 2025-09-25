package com.yju.team2.seilomun.domain.auth.service;

import com.yju.team2.seilomun.domain.auth.dto.SocialLoginRequestDto;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.customer.service.OauthService;
import com.yju.team2.seilomun.domain.customer.oauth.OauthAttribute;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URL;

import java.time.Instant;
import java.util.*;

/**
 * 소셜 로그인(앱 전용)
 * 1) KAKAO/NAVER: code -> token 교환 -> 사용자 정보
 * 2) GOOGLE/APPLE: id_token JWKS 서명 검증 -> claims 추출
 * 3) 정제 사용자 -> 회원 upsert -> JWT(access/refresh) 발급
 *
 * 추가 프로퍼티 파일 없이 application.properties의 spring.security.oauth2.client.* 키만 재사용합니다.
 */
@Service
public class SocialAuthService {

    // ===== Kakao =====
    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoClientSecret;
    @Value("${spring.security.oauth2.client.provider.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String kakaoTokenUri;
    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String kakaoUserInfoUri;

    // ===== Naver =====
    @Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverClientSecret;
    @Value("${spring.security.oauth2.client.provider.naver.token-uri:https://nid.naver.com/oauth2.0/token}")
    private String naverTokenUri;
    @Value("${spring.security.oauth2.client.provider.naver.user-info-uri:https://openapi.naver.com/v1/nid/me}")
    private String naverUserInfoUri;

    // ===== Google (aud 검증용) =====
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    // ===== Apple (aud 검증용; 애플을 쓸 때만 필요) =====
    @Value("${spring.security.oauth2.client.registration.apple.client-id:}")
    private String appleClientId; // 없으면 Apple 로그인 시 에러 메시지로 안내

    private static final String GOOGLE_ISS_1 = "https://accounts.google.com";
    private static final String GOOGLE_ISS_2 = "accounts.google.com";
    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";

    private static final String APPLE_ISS = "https://appleid.apple.com";
    private static final String APPLE_JWKS = "https://appleid.apple.com/auth/keys";

    private final RestTemplate rest = new RestTemplate();
    private final OauthService oauthService;
    private final CustomerRepository customerRepository;
    private final UserStatusService userStatusService;

    public SocialAuthService(
            OauthService oauthService,
            CustomerRepository customerRepository,
            UserStatusService userStatusService
    ) {
        this.oauthService = oauthService;
        this.customerRepository = customerRepository;
        this.userStatusService = userStatusService;
    }

    /** 1) provider별 원본 속성 조회 */
    public Map<String, Object> fetchUserAttributes(SocialLoginRequestDto req) {
        String provider = req.getProvider().toUpperCase();
        return switch (provider) {
            case "KAKAO" -> kakaoAttributes(req.getCode(), req.getRedirectUri());
            case "NAVER" -> naverAttributes(req.getCode(), req.getRedirectUri());
            case "GOOGLE" -> googleAttributesFromIdToken(req.getIdToken());
            case "APPLE" -> appleAttributesFromIdToken(req.getIdToken());
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };
    }

    /** 2) 원본 -> 3) 정제 사용자 정보 */
    public Map<String, Object> toSanitizedUser(String provider, Map<String, Object> raw) {
        provider = provider.toUpperCase();
        Map<String, Object> user = new HashMap<>();

        switch (provider) {
            case "KAKAO": {
                Object id = raw.get("id");
                Map<String, Object> kakaoAccount = safeMap(raw.get("kakao_account"));
                Map<String, Object> profile = safeMap(kakaoAccount.get("profile"));
                user.put("provider", "KAKAO");
                user.put("providerId", id);
                user.put("email", kakaoAccount.get("email"));
                user.put("nickname", profile.getOrDefault("nickname", ""));
                user.put("profileImageUrl", profile.getOrDefault("profile_image_url", ""));
                break;
            }
            case "NAVER": {
                Map<String, Object> resp = safeMap(raw.get("response"));
                user.put("provider", "NAVER");
                user.put("providerId", resp.get("id"));
                user.put("email", resp.get("email"));
                user.put("nickname", resp.getOrDefault("nickname", resp.getOrDefault("name", "")));
                user.put("profileImageUrl", resp.getOrDefault("profile_image", ""));
                break;
            }
            case "GOOGLE": {
                // raw = claims map
                user.put("provider", "GOOGLE");
                user.put("providerId", raw.get("sub"));
                user.put("email", raw.get("email"));
                Object name = raw.getOrDefault("name", "");
                if (name == null || String.valueOf(name).isBlank()) {
                    // fallback: 이메일 앞부분
                    Object email = raw.get("email");
                    name = email != null ? String.valueOf(email).split("@")[0] : "";
                }
                user.put("nickname", name);
                user.put("profileImageUrl", raw.getOrDefault("picture", ""));
                break;
            }
            case "APPLE": {
                // raw = claims map (Apple은 name이 거의 없음)
                user.put("provider", "APPLE");
                user.put("providerId", raw.get("sub"));
                user.put("email", raw.get("email")); // 첫 로그인 때만 내려올 수 있음
                user.put("nickname", "");
                user.put("profileImageUrl", "");
                break;
            }
            default:
                throw new IllegalArgumentException("정제 매핑 미지원 provider: " + provider);
        }
        return user;
    }

    /** 4) 회원 upsert + 5) JWT 발급 (기존 로직 재사용) */
    public Map<String, Object> upsertAndIssueTokens(String provider, Map<String, Object> raw, Map<String, Object> sanitizedUser) {
        String providerLower = provider.toLowerCase();

        OauthAttribute oauthAttr = OauthAttribute.of(providerLower, raw);

        String email = oauthAttr.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일 권한 동의가 필요합니다. (콘솔에서 email 동의 필수 설정)");
        }

        // 신규 가입
        if (customerRepository.findByEmail(email).isEmpty()) {
            oauthService.registerCustomer(
                    oauthAttr.getName(),
                    oauthAttr.getBirthday() == null ? "" : oauthAttr.getBirthday().replace("-", ""),
                    oauthAttr.getEmail(),
                    oauthAttr.getNickname(),
                    oauthAttr.getProfile()
            );
        }

        // JWT 발급
        Map<String, String> tokens = oauthService.customerLogin(email);

        // 온라인 상태 업데이트
        userStatusService.updateOnlineStatus(email, "CUSTOMER");

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", tokens.get("accessToken"));
        result.put("refreshToken", tokens.get("refreshToken"));
        result.put("user", sanitizedUser);
        return result;
    }

    // ===== Kakao: code -> token -> me =====
    private Map<String, Object> kakaoAttributes(String code, String redirectUri) {
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> tokenForm = new LinkedMultiValueMap<>();
        tokenForm.add("grant_type", "authorization_code");
        tokenForm.add("client_id", kakaoClientId);
        tokenForm.add("redirect_uri", redirectUri);
        tokenForm.add("code", code);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            tokenForm.add("client_secret", kakaoClientSecret);
        }

        ResponseEntity<Map> tokenResp = rest.postForEntity(
                kakaoTokenUri, new HttpEntity<>(tokenForm, tokenHeaders), Map.class
        );
        String accessToken = (String) tokenResp.getBody().get("access_token");

        HttpHeaders meHeaders = new HttpHeaders();
        meHeaders.setBearerAuth(accessToken);
        ResponseEntity<Map> meResp = rest.exchange(
                kakaoUserInfoUri, HttpMethod.GET, new HttpEntity<>(meHeaders), Map.class
        );
        return meResp.getBody();
    }

    // ===== Naver: code -> token -> me =====
    private Map<String, Object> naverAttributes(String code, String redirectUri) {
        String tokenUrl = naverTokenUri
                + "?grant_type=authorization_code"
                + "&client_id=" + naverClientId
                + "&client_secret=" + naverClientSecret
                + "&code=" + code
                + "&state=STATE"; // 실제로는 CSRF 대비 state 매칭 권장

        ResponseEntity<Map> tokenResp = rest.getForEntity(tokenUrl, Map.class);
        String accessToken = (String) tokenResp.getBody().get("access_token");

        HttpHeaders meHeaders = new HttpHeaders();
        meHeaders.setBearerAuth(accessToken);
        ResponseEntity<Map> meResp = rest.exchange(
                naverUserInfoUri, HttpMethod.GET, new HttpEntity<>(meHeaders), Map.class
        );
        return meResp.getBody();
    }

    // ===== Google: id_token JWKS 서명 + iss/aud/exp 검증 =====
    private Map<String, Object> googleAttributesFromIdToken(String idToken) {
        try {
            JWTClaimsSet claims = verifyWithJWKS(idToken, GOOGLE_JWKS);

            String iss = claims.getIssuer();
            if (!(GOOGLE_ISS_1.equals(iss) || GOOGLE_ISS_2.equals(iss))) {
                throw new IllegalArgumentException("Google iss 불일치: " + iss);
            }
            if (googleClientId == null || googleClientId.isBlank()) {
                throw new IllegalStateException("google client-id 설정이 없습니다.");
            }
            List<String> aud = claims.getAudience();
            if (aud == null || !aud.contains(googleClientId)) {
                throw new IllegalArgumentException("Google aud 불일치");
            }
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(java.time.Instant.now())) {
                throw new IllegalArgumentException("Google id_token 만료");
            }

            Map<String, Object> raw = new HashMap<>();
            raw.put("sub", claims.getSubject());
            raw.put("email", claims.getClaim("email"));
            raw.put("email_verified", claims.getClaim("email_verified"));
            raw.put("name", claims.getClaim("name"));
            raw.put("picture", claims.getClaim("picture"));
            raw.put("iss", iss);
            raw.put("aud", aud);
            raw.put("exp", exp.getTime() / 1000);
            return raw;

        } catch (Exception e) {
            throw new IllegalArgumentException("Google id_token 검증 실패: " + e.getMessage(), e);
        }
    }


    // ===== Apple: id_token JWKS 서명 + iss/aud/exp 검증 =====
    private Map<String, Object> appleAttributesFromIdToken(String idToken) {
        try {
            if (appleClientId == null || appleClientId.isBlank()) {
                throw new IllegalStateException("apple client-id(=Service ID)가 설정되어 있지 않습니다.");
            }
            JWTClaimsSet claims = verifyWithJWKS(idToken, APPLE_JWKS);

            String iss = claims.getIssuer();
            if (!APPLE_ISS.equals(iss)) {
                throw new IllegalArgumentException("Apple iss 불일치: " + iss);
            }
            List<String> aud = claims.getAudience();
            if (aud == null || !aud.contains(appleClientId)) {
                throw new IllegalArgumentException("Apple aud 불일치");
            }
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(java.time.Instant.now())) {
                throw new IllegalArgumentException("Apple id_token 만료");
            }

            Map<String, Object> raw = new HashMap<>();
            raw.put("sub", claims.getSubject());
            raw.put("email", claims.getClaim("email")); // 첫 로그인 시에만 있을 수 있음
            raw.put("email_verified", claims.getClaim("email_verified"));
            raw.put("iss", iss);
            raw.put("aud", aud);
            raw.put("exp", exp.getTime() / 1000);
            return raw;

        } catch (Exception e) {
            throw new IllegalArgumentException("Apple id_token 검증 실패: " + e.getMessage(), e);
        }
    }


    // ===== 공통: JWKS로 서명 검증 후 Claims 반환 =====
    private JWTClaimsSet verifyWithJWKS(String idToken, String jwksUrl) throws Exception {
        SignedJWT jwt = SignedJWT.parse(idToken);

        // JWKS 리트리버(타임아웃/캐시)
        ResourceRetriever retriever = new DefaultResourceRetriever(2000, 2000, 32768);
        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(jwksUrl), retriever);

        // RS* 알고리듬 패밀리 허용 (Google/Apple 모두 RS256)
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        JWSKeySelector<SecurityContext> keySelector =
                new JWSAlgorithmFamilyJWSKeySelector<>(JWSAlgorithm.Family.RSA, jwkSource);
        processor.setJWSKeySelector(keySelector);

        return processor.process(jwt, null);
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return new HashMap<>();
    }
}
