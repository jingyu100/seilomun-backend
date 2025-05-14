package com.yju.team2.seilomun.domain.notification.dto;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public static NotificationDto fromEntity(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .recipientType(notification.getRecipientType())
                .recipientId(notification.getRecipientId())
                .senderType(notification.getSenderType())
                .senderId(notification.getSenderId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}