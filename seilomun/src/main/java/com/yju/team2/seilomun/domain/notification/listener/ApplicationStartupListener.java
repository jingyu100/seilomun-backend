package com.yju.team2.seilomun.domain.notification.listener;

import com.yju.team2.seilomun.domain.notification.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupListener {

    private final SseEmitterRepository sseEmitterRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void handleApplicationReady() {
        // 서버 시작 시 이전 연결 정보 정리
        String currentServerId = sseEmitterRepository.getCurrentServerId();
        String connectionKey = "sse:connection:" + currentServerId;

        // 이전에 이 서버 ID로 저장된 연결 정보 삭제
        Set<Object> oldConnections = sseEmitterRepository.findAllLocalConnections();
        if (oldConnections != null && !oldConnections.isEmpty()) {
            for (Object customerId : oldConnections) {
                sseEmitterRepository.deleteById(Long.parseLong((String) customerId));
            }
            log.info("서버 시작 시 {} 개의 이전 연결 정보 정리", oldConnections.size());
        }
    }
}