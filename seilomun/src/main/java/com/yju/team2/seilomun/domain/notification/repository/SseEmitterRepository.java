package com.yju.team2.seilomun.domain.notification.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SseEmitterRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // 로컬 메모리에 실제 SseEmitter 객체 저장
    private final Map<Long, SseEmitter> localEmitters = new ConcurrentHashMap<>();

    // Redis 키 프리픽스
    private static final String SSE_CONNECTION_PREFIX = "sse:connection:";
    private static final String SSE_USER_KEY_PREFIX = "sse:user:";
    private static final long SSE_CONNECTION_TTL = 3600; // 1시간

    // 서버 식별자
    private final String serverId = java.util.UUID.randomUUID().toString();

    public void save(Long customerId, SseEmitter emitter) {
        // 로컬 메모리에 저장
        localEmitters.put(customerId, emitter);

        // Redis에 연결 정보 저장 (사용자ID -> 서버ID 매핑)
        String userKey = SSE_USER_KEY_PREFIX + customerId;
        redisTemplate.opsForValue().set(userKey, serverId, SSE_CONNECTION_TTL, TimeUnit.SECONDS);

        // 서버별 연결 목록에 추가
        String connectionKey = SSE_CONNECTION_PREFIX + serverId;
        redisTemplate.opsForSet().add(connectionKey, customerId.toString());
        redisTemplate.expire(connectionKey, SSE_CONNECTION_TTL, TimeUnit.SECONDS);

        log.info("SSE 연결 저장: customerId={}, serverId={}", customerId, serverId);
    }

    public SseEmitter findLocalEmitter(Long customerId) {
        return localEmitters.get(customerId);
    }

    public void deleteById(Long customerId) {
        // 로컬 메모리에서 삭제
        localEmitters.remove(customerId);

        // Redis에서 삭제
        String userKey = SSE_USER_KEY_PREFIX + customerId;
        Object storedServerId = redisTemplate.opsForValue().get(userKey);

        if (storedServerId != null) {
            redisTemplate.delete(userKey);

            // 서버별 연결 목록에서 제거
            String connectionKey = SSE_CONNECTION_PREFIX + storedServerId;
            redisTemplate.opsForSet().remove(connectionKey, customerId.toString());
        }

        log.info("SSE 연결 삭제: customerId={}", customerId);
    }

    // 특정 사용자가 어느 서버에 연결되어 있는지 확인
    public String findServerIdByCustomerId(Long customerId) {
        String userKey = SSE_USER_KEY_PREFIX + customerId;
        return (String) redisTemplate.opsForValue().get(userKey);
    }

    // 현재 서버의 모든 연결된 사용자 ID 조회
    public Set<Object> findAllLocalConnections() {
        String connectionKey = SSE_CONNECTION_PREFIX + serverId;
        return redisTemplate.opsForSet().members(connectionKey);
    }

    // 모든 서버의 연결 정보 조회
    public Set<String> findAllServerIds() {
        Set<String> keys = redisTemplate.keys(SSE_CONNECTION_PREFIX + "*");
        Set<String> serverIds = new java.util.HashSet<>();

        if (keys != null) {
            for (String key : keys) {
                serverIds.add(key.replace(SSE_CONNECTION_PREFIX, ""));
            }
        }

        return serverIds;
    }

    public String getCurrentServerId() {
        return serverId;
    }
}
