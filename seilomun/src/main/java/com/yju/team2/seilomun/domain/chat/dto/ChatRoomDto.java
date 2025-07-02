package com.yju.team2.seilomun.domain.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomDto {
    private Long id;
    private Long customerId;
    private String customerNickname;
    private Long sellerId;
    private String sellerStoreName;
    private LocalDateTime createdAt;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String sellerPhotoUrl;
    private String customerPhotoUrl;
    private int unreadCount;
}