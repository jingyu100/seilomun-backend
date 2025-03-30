package com.yju.team2.seilomun.domain.auth;


import com.yju.team2.seilomun.domain.auth.RefreshTokenService;
import com.yju.team2.seilomun.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OauthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;


    // 네이버에서 AccessToken을 가져오는 메서드
    public String getAccessTokenFromNaver(String code) throws IOException {
        log.info("네이버 OAuth 코드 수신: {}", code);
        // 네이버 토큰 발급 API URL
        String tokenUrl = "https://nid.naver.com/oauth2.0/token";

        // 네이버 로그인에 필요한 클라이언트 ID와 시크릿
        String clientId = "KA0cHEgzNKssD2mUVAvw";  // 실제 클라이언트 ID
        String clientSecret = "zSgaAtteVm";  // 실제 클라이언트 Secret
        String redirectUri = "http://localhost:80/login/oauth2/code/naver";  // 리다이렉트 URI

        // 토큰 발급을 위한 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        // RestTemplate을 사용하여 POST 요청 보내기
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        // 토큰 발급 API 호출
        ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, String.class);

        log.info("네이버 토큰 API 응답: {}", response.getBody());

        // 네이버에서 받은 응답을 JSON으로 파싱하여 AccessToken 추출
        if (response.getStatusCode() == HttpStatus.OK) {
            String responseBody = response.getBody();
            return parseAccessTokenFromResponse(responseBody);
        } else {
            throw new RuntimeException("Failed to get access token from Naver");
        }
    }

    // 응답에서 access_token을 파싱하는 메서드 (Jackson 사용)
    private String parseAccessTokenFromResponse(String responseBody) throws IOException {
        JsonNode jsonResponse = objectMapper.readTree(responseBody); // JSON 문자열을 JsonNode로 파싱
        return jsonResponse.get("access_token").asText(); // access_token 필드 값 반환
    }

    // 네이버 사용자 정보를 가져오는 메서드
    public Map<String, Object> getUserInfoFromNaver(String accessToken) throws IOException {
        log.info("네이버 사용자 정보 요청 시작, 액세스 토큰: {}", accessToken);


        // 네이버 사용자 정보 API URL
        String userInfoUrl = "https://openapi.naver.com/v1/nid/me";

        // RestTemplate을 사용하여 GET 요청 보내기
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 사용자 정보 API 호출
        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);

        log.info("네이버 사용자 정보 API 응답: {}", response.getBody());

        // 응답에서 사용자 정보를 JSON으로 파싱
        if (response.getStatusCode() == HttpStatus.OK) {
            String responseBody = response.getBody();
            return parseUserInfoFromResponse(responseBody);
        } else {
            throw new RuntimeException("Failed to get user info from Naver");
        }
    }

    // 네이버에서 받은 응답에서 사용자 정보를 파싱하는 메서드
    private Map<String, Object> parseUserInfoFromResponse(String responseBody) throws IOException {
        log.info("사용자 정보 응답 파싱 시작: {}", responseBody);  // 응답 로그

        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        if (!jsonResponse.has("response")) {
            log.error("응답에 사용자 정보가 없습니다.");
            throw new RuntimeException("User information is missing in the response");
        }

        JsonNode responseNode = jsonResponse.get("response");
        String email = responseNode.get("email").asText();
        String name = responseNode.get("name").asText();

        log.info("파싱된 사용자 이메일: {}, 이름: {}", email, name);  // 파싱된 이메일 및 이름 로그

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", email);
        userInfo.put("name", name);

        return userInfo;
    }

    // JWT 토큰 발급 및 리프레시 토큰 저장
    public Map<String, String> generateJwtForUser(String email) {
        String userType = "CUSTOMER"; // 예시로 "CUSTOMER"로 설정
        String accessToken = jwtUtil.generateAccessToken(email, userType);
        String refreshToken = refreshTokenService.getRefreshToken(email);

        // 리프레시 토큰이 없다면 새로운 리프레시 토큰을 생성하고 저장
        if (refreshToken == null) {
            refreshToken = jwtUtil.generateRefreshToken(email, userType);
            refreshTokenService.saveRefreshToken(email, userType, refreshToken);
        }

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }
}
