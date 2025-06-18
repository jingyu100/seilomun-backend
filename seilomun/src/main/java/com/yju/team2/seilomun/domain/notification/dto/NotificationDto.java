package com.yju.team2.seilomun.domain.notification.dto;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {

    private Long id;
    private String content;
    private Character isRead;
    private Character recipientType;
    private Long recipientId;
    private Character senderType;
    private Long senderId;
    private LocalDateTime createdAt;

    // 라우팅을 위한 추가 필드들
    private Long refundId;
    private Long productId;
    private String orderNumber;
    private String notificationType;

    public static NotificationDto fromEntity(Notification notification) {
        NotificationDto dto = NotificationDto.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .recipientType(notification.getRecipientType())
                .recipientId(notification.getRecipientId())
                .senderType(notification.getSenderType())
                .senderId(notification.getSenderId())
                .createdAt(notification.getCreatedAt())
                .build();

        // 알림 내용에서 관련 ID들 추출
        extractIdsFromContent(dto);

        return dto;
    }

    // 알림 내용에서 ID 정보 추출하는 메서드
    private static void extractIdsFromContent(NotificationDto dto) {
        String content = dto.getContent();
        if (content == null) return;

        // 주문번호 추출 (주문번호: ABC123 형태)
        Pattern orderNumberPattern = Pattern.compile("주문번호:\\s*([A-Z0-9]+)");
        Matcher orderNumberMatcher = orderNumberPattern.matcher(content);
        if (orderNumberMatcher.find()) {
            dto.setOrderNumber(orderNumberMatcher.group(1));
        }

        // 환불번호 추출 (환불번호: 123 형태) - 환불ID → 환불번호로 변경
        Pattern refundIdPattern = Pattern.compile("환불번호:\\s*(\\d+)");
        Matcher refundIdMatcher = refundIdPattern.matcher(content);
        if (refundIdMatcher.find()) {
            dto.setRefundId(Long.parseLong(refundIdMatcher.group(1)));
        }

        // 상품ID는 현재 NotificationUtil에서 사용하지 않으므로 제거하거나
        // 필요하다면 상품번호로 변경할 수 있습니다.

        // 알림 타입 결정
        dto.setNotificationType(determineNotificationType(content));
    }

    // 알림 내용으로부터 타입 결정
    private static String determineNotificationType(String content) {
        if (content.contains("주문이 들어왔습니다") || content.contains("주문을 수락") || content.contains("주문을 거절")) {
            return "ORDER";
        } else if (content.contains("환불")) {
            return "REFUND";
        } else if (content.contains("리뷰")) {
            return "REVIEW";
        } else if (content.contains("상품")) {
            return "PRODUCT";
        } else if (content.contains("결제")) {
            return "PAYMENT";
        } else {
            return "GENERAL";
        }
    }
}