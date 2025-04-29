package com.yju.team2.seilomun.domain.chat.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.yju.team2.seilomun.domain.chat.dto.ChatMessageDto;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriber implements MessageListener {

    private final RedisTemplate<String, Object> chatRedisTemplate;
    private final SimpMessageSendingOperations messagingTemplate;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Redis에서 받은 데이터를 역직렬화 (문자열로 받기)
            String publishMessage = chatRedisTemplate.getStringSerializer().deserialize(message.getBody());

            log.debug("수신된 메시지: {}", publishMessage);

            // 새로운 ObjectMapper 생성하여 설정
            ObjectMapper customMapper = new ObjectMapper();
            customMapper.findAndRegisterModules(); // LocalDateTime 처리용
            customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // 직접 역직렬화
            ChatMessageDto chatMessage = customMapper.readValue(publishMessage, ChatMessageDto.class);

            // WebSocket 구독자에게 메시지 전송
            messagingTemplate.convertAndSend(
                    "/queue/messages/" + chatMessage.getChatRoomId(),
                    chatMessage);


            log.debug("메시지 전송 완료: {}", chatMessage);
        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생: {}", e.getMessage(), e);
            // 전체 메시지를 로그에 기록하여 디버깅
            String fullMessage = chatRedisTemplate.getStringSerializer().deserialize(message.getBody());
            log.error("원본 메시지: {}", fullMessage);
        }
    }
}