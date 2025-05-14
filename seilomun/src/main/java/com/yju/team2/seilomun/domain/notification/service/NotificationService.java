package com.yju.team2.seilomun.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.entity.Favorite;
import com.yju.team2.seilomun.domain.customer.repository.FavoriteRepository;
import com.yju.team2.seilomun.domain.notification.dto.NotificationDto;
import com.yju.team2.seilomun.domain.notification.dto.NotificationMessage;
import com.yju.team2.seilomun.domain.notification.entity.Notification;
import com.yju.team2.seilomun.domain.notification.repository.NotificationRepository;
import com.yju.team2.seilomun.domain.notification.repository.SseEmitterRepository;
import com.yju.team2.seilomun.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SseEmitterRepository sseEmitterRepository;
    private final NotificationRepository notificationRepository;
    private final FavoriteRepository favoriteRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 60분
    private static final String NOTIFICATION_CHANNEL = "notification:channel";

    // SSE 연결
    public SseEmitter connect(Long customerId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // Redis에 연결 정보 저장
        sseEmitterRepository.save(customerId, emitter);

        // 이벤트 핸들러 설정
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: customerId={}", customerId);
            sseEmitterRepository.deleteById(customerId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: customerId={}", customerId);
            sseEmitterRepository.deleteById(customerId);
        });

        emitter.onError((e) -> {
            log.error("SSE 에러 발생: customerId={}", customerId, e);
            sseEmitterRepository.deleteById(customerId);
        });

        // 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("연결이 성공적으로 설정되었습니다."));
        } catch (IOException e) {
            log.error("더미 이벤트 전송 실패: customerId={}", customerId, e);
            sseEmitterRepository.deleteById(customerId);
        }

        return emitter;
    }

    // 새로운 상품 등록 시 알림 전송
    @Transactional
    public void notifyNewProduct(Product product) {
        Long sellerId = product.getSeller().getId();

        // 해당 판매자를 즐겨찾기한 모든 고객 조회
        List<Favorite> favorites = favoriteRepository.findBySellerId(sellerId);

        for (Favorite favorite : favorites) {
            Customer customer = favorite.getCustomer();
            Long customerId = customer.getId();

            // 알림 저장
            Notification notification = Notification.builder()
                    .content(String.format("%s 매장에 새로운 상품 '%s'이(가) 등록되었습니다.",
                            product.getSeller().getStoreName(), product.getName()))
                    .isVisible('Y')
                    .recipientType('C')
                    .recipientId(customerId)
                    .senderType('S')
                    .senderId(sellerId)
                    .isRead('N')
                    .build();

            notificationRepository.save(notification);

            // Redis Pub/Sub으로 알림 메시지 발행
            NotificationMessage message = NotificationMessage.builder()
                    .customerId(customerId)
                    .notification(NotificationDto.fromEntity(notification))
                    .build();

            publishNotification(message);
        }
    }

    // Redis Pub/Sub으로 알림 발행
    private void publishNotification(NotificationMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, jsonMessage);
            log.info("알림 발행: customerId={}", message.getCustomerId());
        } catch (Exception e) {
            log.error("알림 발행 실패", e);
        }
    }

    // 로컬 SSE 이미터로 알림 전송
    public void sendLocalNotification(Long customerId, NotificationDto notificationDto) {
        SseEmitter emitter = sseEmitterRepository.findLocalEmitter(customerId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificationDto));
                log.info("로컬 알림 전송 성공: customerId={}", customerId);
            } catch (IOException e) {
                log.error("로컬 알림 전송 실패: customerId={}", customerId, e);
                sseEmitterRepository.deleteById(customerId);
            }
        }
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, Long customerId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));

        if (!notification.getRecipientId().equals(customerId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        notification.updateIsRead('Y');
        notificationRepository.save(notification);
    }
}