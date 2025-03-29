package com.yju.team2.seilomun.dto;

import java.io.IOException;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
    private Long id;
    private MessageType type;
    private Long chatRoomId;
    private Long senderId;
    private Long receiverId;
    // 보낸 사람 이름 판매자면 상점명, 고객이면 닉네임
    private String senderName;
    private Character senderType;
    private String content;
    private LocalDateTime timestamp;
    private Character read;


    @JsonCreator
    public static ChatMessageDto create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, ChatMessageDto.class);
    }
}