package com.yju.team2.seilomun.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.auth.dto.BusinessVerificationRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BusinessVerificationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bno.serviceKey}")
    private String serviceKey;

    // 국세청 사업자등록정보 조회 API를 통해 사업자 정보 검증
    public Map<String, Object> verifyBusiness(BusinessVerificationRequestDto requestDto) {
        log.info("Received request: bNo={}, startDt={}, pNm={}",
                requestDto.getB_no(), requestDto.getStart_dt(), requestDto.getP_nm());

        log.info("Using original service key: {}", serviceKey);

        Map<String, Object> resultMap = new HashMap<>();

        try {
            // 필수 필드 검증
            if (requestDto.getB_no() == null || requestDto.getStart_dt() == null || requestDto.getP_nm() == null) {
                resultMap.put("status", HttpStatus.BAD_REQUEST);
                resultMap.put("error", "필수 필드 누락");
                resultMap.put("message", "사업자번호(b_no), 개업일자(start_dt), 대표자명(p_nm)은 필수입니다.");
                return resultMap;
            }

            // API 요청 본문 생성
            Map<String, List<Map<String, String>>> requestBody = new HashMap<>();
            Map<String, String> business = new HashMap<>();

            business.put("b_no", requestDto.getB_no());
            business.put("start_dt", requestDto.getStart_dt());
            business.put("p_nm", requestDto.getP_nm());
            business.put("p_nm2", requestDto.getP_nm2() != null ? requestDto.getP_nm2() : "");
            business.put("b_nm", requestDto.getB_nm() != null ? requestDto.getB_nm() : "");
            business.put("corp_no", requestDto.getCorp_no() != null ? requestDto.getCorp_no() : "");
            business.put("b_sector", requestDto.getB_sector() != null ? requestDto.getB_sector() : "");
            business.put("b_type", requestDto.getB_type() != null ? requestDto.getB_type() : "");
            business.put("b_adr", requestDto.getB_adr() != null ? requestDto.getB_adr() : "");

            List<Map<String, String>> businesses = new ArrayList<>();
            businesses.add(business);
            requestBody.put("businesses", businesses);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            log.info("API Request Body: {}", requestBodyJson);

            // 직접 URL 문자열 구성 (인코딩된 키 그대로 사용)
            String baseUrl = "http://api.odcloud.kr/api/nts-businessman/v1/validate";
            String urlString = baseUrl + "?serviceKey=" + serviceKey;
            log.info("Using API URL: {}", urlString);

            // HttpURLConnection 직접 사용
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // 요청 본문 전송
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 응답 처리
            int responseCode = connection.getResponseCode();
            log.info("API Response Code: {}", responseCode);

            // 응답 본문 읽기
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBody.append(line);
                }
            }

            String responseBodyStr = responseBody.toString();
            log.info("API Response Body: {}", responseBodyStr);

            if (responseCode >= 200 && responseCode < 300) {
                JsonNode responseNode = objectMapper.readTree(responseBodyStr);
                boolean isValid = false;
                if (responseNode.has("valid_cnt")) {
                    int validCnt = responseNode.get("valid_cnt").asInt();
                    isValid = (validCnt > 0);
                }

                resultMap.put("status", HttpStatus.OK);
                resultMap.put("isValid", isValid);
            } else {
                resultMap.put("status", HttpStatus.valueOf(responseCode));
                resultMap.put("error", "API 응답 오류");
                resultMap.put("message", responseBodyStr);
            }

        } catch (Exception e) {
            log.error("사업자 검증 중 오류 발생: {}", e.getMessage(), e);
            resultMap.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
            resultMap.put("error", "서버 오류");
            resultMap.put("message", e.getMessage());
        }

        return resultMap;
    }
}