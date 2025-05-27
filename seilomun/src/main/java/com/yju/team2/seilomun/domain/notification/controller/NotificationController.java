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

    // SSE 연결 - 고객용
    @GetMapping(value = "/customer/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        String userType = userDetails.getUserType();

        if (!"CUSTOMER".equals(userType)) {
            throw new IllegalArgumentException("소비자만 알림을 구독할 수 있습니다.");
        }

        log.info("SSE 연결 요청: customerId={}", userId);
        return notificationService.connect(userId);
    }

    // SSE 연결 - 판매자용 (새로 추가)
    @GetMapping(value = "/seller/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSeller(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        String userType = userDetails.getUserType();

        if (!"SELLER".equals(userType)) {
            throw new IllegalArgumentException("판매자만 이 연결을 사용할 수 있습니다.");
        }

        log.info("판매자 SSE 연결 요청: sellerId={}", userId);
        return notificationService.connectSeller(userId);
    }

    // 알림 읽음 처리
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponseJson> markAsRead(@PathVariable Long notificationId,
                                                      @AuthenticationPrincipal JwtUserDetails userDetails) {
        try {
            notificationService.markAsRead(notificationId, userDetails.getId());
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("message", "알림이 읽음 처리되었습니다.")));
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: notificationId={}", notificationId, e);
            return ResponseEntity.badRequest().body(new ApiResponseJson(HttpStatus.BAD_REQUEST,
                    Map.of("error", e.getMessage())));
        }
    }

    // 알림 목록 출력
    @GetMapping("/list")
    public ResponseEntity<ApiResponseJson> getNotifications(@AuthenticationPrincipal JwtUserDetails userDetails) {
        List<Notification> notificationList = notificationService.getNotifications(userDetails.getId(), userDetails.getUserType());
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "notifications", notificationList,
                "count", notificationList.size())));
    }

    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponseJson> getUnreadCount(@AuthenticationPrincipal JwtUserDetails userDetails) {
        int unreadCount = notificationService.getUnreadCount(userDetails.getId(), userDetails.getUserType());
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "unreadCount", unreadCount)));
    }

    // 모든 알림 읽음 처리
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponseJson> markAllAsRead(@AuthenticationPrincipal JwtUserDetails userDetails) {
        try {
            int updatedCount = notificationService.markAllAsRead(userDetails.getId(), userDetails.getUserType());
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                    "message", "모든 알림이 읽음 처리되었습니다.",
                    "updatedCount", updatedCount)));
        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 실패: userId={}", userDetails.getId(), e);
            return ResponseEntity.badRequest().body(new ApiResponseJson(HttpStatus.BAD_REQUEST,
                    Map.of("error", e.getMessage())));
        }
    }
}