package com.yju.team2.seilomun.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.notification.dto.NotificationMessage;
import com.yju.team2.seilomun.domain.notification.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final SseEmitterRepository sseEmitterRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonMessage = new String(message.getBody());
            NotificationMessage notificationMessage = objectMapper.readValue(jsonMessage, NotificationMessage.class);

            Long customerId = notificationMessage.getCustomerId();

            // 현재 서버에 해당 고객이 연결되어 있는지 확인
            String serverIdOfCustomer = sseEmitterRepository.findServerIdByCustomerId(customerId);
            String currentServerId = sseEmitterRepository.getCurrentServerId();

            if (currentServerId.equals(serverIdOfCustomer)) {
                // 현재 서버에 연결된 고객이면 직접 전송
                notificationService.sendLocalNotification(customerId, notificationMessage.getNotification());
            }

        } catch (Exception e) {
            log.error("알림 메시지 처리 실패", e);
        }
    }
}
