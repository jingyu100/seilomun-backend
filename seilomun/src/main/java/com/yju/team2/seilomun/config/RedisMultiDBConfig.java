package com.yju.team2.seilomun.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisMultiDBConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    // 기본 redis 연결 팩토리 (DB 0)
    // 기본 사용자 상태 or 일반적인 데이터
    @Primary
    @Bean("defaultRedisConnectionFactory")
    public RedisConnectionFactory defaultRedisConnectionFactory() {
        return createConnectionFactory(0);
    }

    // 캐시용 Redis 연결 팩토리 (DB 1)
    // 상품 할인율 등 캐시 데이터
    @Bean("cacheRedisConnectionFactory")
    public RedisConnectionFactory cacheRedisConnectionFactory() {
        return createConnectionFactory(1);
    }

    // 세션용 Redis 연결 팩토리 (DB 2)
    // 세션, JWT 토큰 관리
    @Bean("sessionRedisConnectionFactory")
    public RedisConnectionFactory sessionRedisConnectionFactory() {
        return createConnectionFactory(2);
    }

    // 알림용 Redis 연결 팩토리 (DB 3)
    // SSE 연결, 알림 관리
    @Bean("notificationRedisConnectionFactory")
    public RedisConnectionFactory notificationRedisConnectionFactory() {
        return createConnectionFactory(3);
    }

    // 검색용 Redis 연결 팩토리 (DB 4)
    // 검색 기록, 인기 검색어
    @Bean("searchRedisConnectionFactory")
    public RedisConnectionFactory searchRedisConnectionFactory() {
        return createConnectionFactory(4);
    }

    // 장바구니용 Redis 연결 팩토리 (DB 5)
    // 장바구니 데이터
    @Bean("cartRedisConnectionFactory")
    public RedisConnectionFactory cartRedisConnectionFactory() {
        return createConnectionFactory(5);
    }

    // 채팅용 Redis 연결 팩토리 (DB 6)
    // 채팅 활성 상태
    @Bean("chatRedisConnectionFactory")
    public RedisConnectionFactory chatRedisConnectionFactory() {
        return createConnectionFactory(6);
    }

    private RedisConnectionFactory createConnectionFactory(int database) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        factory.setHostName(host);
        factory.setPort(port);
        factory.setDatabase(database);
        factory.afterPropertiesSet();
        return factory;
    }
}
