package com.yju.team2.seilomun.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.customer.repository.FavoriteRepository;
import com.yju.team2.seilomun.domain.notification.dto.NotificationDto;
import com.yju.team2.seilomun.domain.notification.dto.NotificationMessage;
import com.yju.team2.seilomun.domain.notification.entity.Notification;
import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.notification.repository.NotificationRepository;
import com.yju.team2.seilomun.domain.notification.repository.SseEmitterRepository;
import com.yju.team2.seilomun.domain.notification.strategy.NotificationStrategy;
import com.yju.team2.seilomun.domain.notification.strategy.NotificationStrategyFactory;
import com.yju.team2.seilomun.domain.notification.util.NotificationUtil;
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
    private final NotificationStrategyFactory strategyFactory;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 60분
    private static final String NOTIFICATION_CHANNEL = "notification:channel";

    // 테스트용 알림 전송 - 고객
    @Transactional
    public void sendTestNotificationToCustomer(Long customerId, String message) {
        try {
            // 테스트 알림 생성
            Notification notification = Notification.builder()
                    .content(message)
                    .isVisible('Y')
                    .recipientType('C') // Customer
                    .recipientId(customerId)
                    .senderType('S') // System
                    .senderId(0L) // 시스템 발송
                    .isRead('N')
                    .build();

            // DB에 저장
            notificationRepository.save(notification);

            // Redis Pub/Sub으로 알림 메시지 발행
            NotificationMessage notificationMessage = NotificationMessage.builder()
                    .customerId(customerId)
                    .notification(NotificationDto.fromEntity(notification))
                    .build();

            publishNotification(notificationMessage);

            log.info("테스트 알림 전송 완료: customerId={}, message={}", customerId, message);

        } catch (Exception e) {
            log.error("테스트 알림 전송 실패: customerId={}", customerId, e);
            throw new RuntimeException("테스트 알림 전송에 실패했습니다.", e);
        }
    }

    // 테스트용 알림 전송 - 판매자
    @Transactional
    public void sendTestNotificationToSeller(Long sellerId, String message) {
        try {
            // 테스트 알림 생성
            Notification notification = Notification.builder()
                    .content(message)
                    .isVisible('Y')
                    .recipientType('S') // Seller
                    .recipientId(sellerId)
                    .senderType('S') // System/Seller
                    .senderId(0L) // 시스템 발송
                    .isRead('N')
                    .build();

            // DB에 저장
            notificationRepository.save(notification);

            // Redis Pub/Sub으로 알림 메시지 발행
            NotificationMessage notificationMessage = NotificationMessage.builder()
                    .customerId(sellerId) // 실제로는 sellerId지만 기존 구조 유지
                    .notification(NotificationDto.fromEntity(notification))
                    .build();

            publishNotification(notificationMessage);

            log.info("테스트 알림 전송 완료: sellerId={}, message={}", sellerId, message);

        } catch (Exception e) {
            log.error("테스트 알림 전송 실패: sellerId={}", sellerId, e);
            throw new RuntimeException("테스트 알림 전송에 실패했습니다.", e);
        }
    }

    // 고객용 SSE 연결
    public SseEmitter connect(Long customerId) {
        return createSseConnection(customerId, "CUSTOMER");
    }

    // 판매자용 SSE 연결
    public SseEmitter connectSeller(Long sellerId) {
        return createSseConnection(sellerId, "SELLER");
    }

    // SSE 연결 공통 로직
    private SseEmitter createSseConnection(Long userId, String userType) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // Redis에 연결 정보 저장
        sseEmitterRepository.save(userId, emitter);

        // 이벤트 핸들러 설정
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}, userType={}", userId, userType);
            sseEmitterRepository.deleteById(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}, userType={}", userId, userType);
            sseEmitterRepository.deleteById(userId);
        });

        emitter.onError((e) -> {
            log.error("SSE 에러 발생: userId={}, userType={}", userId, userType, e);
            sseEmitterRepository.deleteById(userId);
        });

        // 연결 확인 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(String.format("%s 알림 연결이 성공적으로 설정되었습니다.",
                            userType.equals("CUSTOMER") ? "고객" : "판매자")));
        } catch (IOException e) {
            log.error("연결 확인 이벤트 전송 실패: userId={}, userType={}", userId, userType, e);
            sseEmitterRepository.deleteById(userId);
        }

        return emitter;
    }

    // 범용적인 알림 처리 메서드
    @Transactional
    public void processNotification(NotificationEvent event) {
        try {
            // 전략 패턴을 사용하여 수신자 결정
            NotificationStrategy strategy = strategyFactory.getStrategy(event.getType());
            List<Long> recipientIds = strategy.getRecipients(event);
            Character recipientType = strategy.getRecipientType();

            log.info("알림 처리 시작: eventType={}, recipientCount={}",
                    event.getType(), recipientIds.size());

            for (Long recipientId : recipientIds) {
                // 알림 생성
                Notification notification = NotificationUtil.createNotification(event, recipientId, recipientType);

                notificationRepository.save(notification);

                // Redis Pub/Sub으로 알림 메시지 발행
                NotificationMessage message = NotificationMessage.builder()
                        .customerId(recipientId)
                        .notification(NotificationDto.fromEntity(notification))
                        .build();

                publishNotification(message);
            }

            log.info("알림 처리 완료: eventId={}, recipientCount={}",
                    event.getEventId(), recipientIds.size());

        } catch (Exception e) {
            log.error("알림 처리 중 오류 발생: eventId={}", event.getEventId(), e);
        }
    }

    // Redis Pub/Sub으로 알림 발행
    private void publishNotification(NotificationMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, jsonMessage);
            log.debug("알림 발행: recipientId={}", message.getCustomerId());
        } catch (Exception e) {
            log.error("알림 발행 실패", e);
        }
    }

    // 로컬 SSE 이미터로 알림 전송
    public void sendLocalNotification(Long userId, NotificationDto notificationDto) {
        SseEmitter emitter = sseEmitterRepository.findLocalEmitter(userId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificationDto));
                log.info("로컬 알림 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("로컬 알림 전송 실패: userId={}", userId, e);
                sseEmitterRepository.deleteById(userId);
            }
        }
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));

        if (!notification.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        notification.updateIsRead('Y');
        notificationRepository.save(notification);

        log.info("알림 읽음 처리: notificationId={}, userId={}", notificationId, userId);
    }

    // 모든 알림 읽음 처리
    @Transactional
    public int markAllAsRead(Long userId, String userType) {
        Character recipientType = userType.equals("CUSTOMER") ? 'C' : 'S';

        List<Notification> unreadNotifications = notificationRepository
                .findUnreadNotificationsByRecipient(userId, recipientType);

        int updatedCount = 0;
        for (Notification notification : unreadNotifications) {
            notification.updateIsRead('Y');
            updatedCount++;
        }

        if (updatedCount > 0) {
            notificationRepository.saveAll(unreadNotifications);
            log.info("모든 알림 읽음 처리 완료: userId={}, userType={}, count={}",
                    userId, userType, updatedCount);
        }

        return updatedCount;
    }

    // 읽지 않은 알림 개수 조회
    public int getUnreadCount(Long userId, String userType) {
        Character recipientType = userType.equals("CUSTOMER") ? 'C' : 'S';
        return notificationRepository.countUnreadNotificationsByRecipient(userId, recipientType);
    }

    // 알림 목록 조회
    public List<Notification> getNotifications(Long userId, String userType) {
        Character recipientType = userType.equals("CUSTOMER") ? 'C' : 'S';
        return notificationRepository.findAllByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(userId, recipientType);
    }

    // 페이징된 알림 목록 조회
    public List<Notification> getNotifications(Long userId, String userType, int page, int size) {
        Character recipientType = userType.equals("CUSTOMER") ? 'C' : 'S';
        return notificationRepository.findNotificationsByRecipientWithPaging(userId, recipientType, page * size, size);
    }
}