package com.yju.team2.seilomun.domain.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebSocketEventListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;

    // 웹소켓 연결할때 발생하는 이벤트
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket 연결 이벤트: {}", stompHeaderAccessor);
    }

    // 웹소켓 구독할때 발생하는 이벤트
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(event.getMessage());
        // 어떤 경로를 구독했는지를 문자열로 저장
        String destination = stompHeaderAccessor.getDestination();
        // 그 경로가 /queue/messages/ 이거 맞는지 확인
        if (destination != null && destination.startsWith("/queue/messages/")) {
            // /queue/messages/ 제외한 문자 가져와서 저장
            String chatRoomId = destination.replace("/queue/messages/", "");

            // 세션 ID를 임시 저장
            String sessionId = stompHeaderAccessor.getSessionId();

            // 구독요청에서 사용자id랑 타입 가져옴
            String userId = stompHeaderAccessor.getFirstNativeHeader("userId");
            String userType = stompHeaderAccessor.getFirstNativeHeader("userType");

            if (userId != null && userType != null) {
                // 세션 정보 저장 - 나중에 연결 종료 시 사용
                String sessionKey = "session:" + sessionId;
                // redis에 Hash 자료 구조로 저장
                // 나중에 연결 종료할때 어떤 사용자가 어떤 채팅방에서 나갔는지 찾기위해
                redisTemplate.opsForHash().put(sessionKey, "chatRoomId", chatRoomId);
                redisTemplate.opsForHash().put(sessionKey, "userId", userId);
                redisTemplate.opsForHash().put(sessionKey, "userType", userType);

                log.info("사용자 {}(타입:{}) 세션 {} 채팅방 {} 구독", userId, userType, sessionId, chatRoomId);
            }
        }
    }

    // 웹소켓 연결 종료할때 이벤트
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = stompHeaderAccessor.getSessionId();

        log.info("WebSocket 연결 종료 이벤트: sessionId={}", sessionId);

        if (sessionId != null) {
            // 세션 정보 키
            String sessionKey = "session:" + sessionId;

            // redis에서 해당 세션의 정보 조회
            String chatRoomId = (String) redisTemplate.opsForHash().get(sessionKey, "chatRoomId");
            String userId = (String) redisTemplate.opsForHash().get(sessionKey, "userId");
            String userType = (String) redisTemplate.opsForHash().get(sessionKey, "userType");

            if (chatRoomId != null && userId != null && userType != null) {
                // 해당 사용자의 키만 삭제
                String key = "chat_active:" + chatRoomId + ":" + userId + ":" + userType;
                redisTemplate.delete(key);

                log.info("사용자 {}(타입:{}) 채팅방 {} 연결 종료 처리 완료", userId, userType, chatRoomId);

                // 세션 정보도 삭제
                redisTemplate.delete(sessionKey);
            }
        }
    }
}