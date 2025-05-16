package com.yju.team2.seilomun.domain.auth.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.auth.dto.UserStatusDto;
import com.yju.team2.seilomun.domain.auth.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserStatusController {

    private final UserStatusService userStatusService;
    
    //이거는 상대 유저의 온라인 상태 확인하는 요청
    @GetMapping("/status/{userType}/{userId}")
    public ResponseEntity<ApiResponseJson> getUserStatus(@PathVariable String userType,
                                                         @PathVariable Long userId) {
        UserStatusDto status = userStatusService.getUserStatus(userId, userType);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("userType", userType);
        response.put("isAvailable", status.isAvailable());
        response.put("status", status.getStatus());

        // 판매자인 경우 영업시간 정보도 포함
        if ("SELLER".equals(userType) && status.getOperatingHours() != null) {
            response.put("operatingHours", status.getOperatingHours());
        }

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, response));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponseJson> heartbeat(@AuthenticationPrincipal JwtUserDetails user) {
        if (user != null) {
            userStatusService.updateOnlineStatus(user.getEmail(), user.getUserType());
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, "heartbeat updated"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}