package com.yju.team2.seilomun.domain.notification.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.notification.entity.Notification;
import com.yju.team2.seilomun.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // SSE 연결
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        String userType = userDetails.getUserType();

        if (!"CUSTOMER".equals(userType)) {
            throw new IllegalArgumentException("소비자만 알림을 구독할 수 있습니다.");
        }

        log.info("SSE 연결 요청: customerId={}", userId);
        return notificationService.connect(userId);
    }

    // 알림 읽음 처리
    @PutMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable Long notificationId,
                           @AuthenticationPrincipal JwtUserDetails userDetails) {
        notificationService.markAsRead(notificationId, userDetails.getId());
    }

    // 알림 목록 출력
    @GetMapping("/list")
    public ResponseEntity<ApiResponseJson> getNotifications(@AuthenticationPrincipal JwtUserDetails userDetails) {
        List<Notification> notificationList = notificationService.getNotifications(userDetails.getId(), userDetails.getUserType());
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "list", notificationList)));
    }
}