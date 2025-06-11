//package com.yju.team2.seilomun.config;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//@Configuration
//public class RedisMultiDBConfig {
//
//    @Value("${spring.redis.host}")
//    private String host;
//
//    @Value("${spring.data.redis.port}")
//    private int port;
//
//    // 기본 redis 연결 팩토리 (DB 0)
//    // 기본 사용자 상태 or 일반적인 데이터
//    @Primary
//    @Bean("defaultRedisConnectionFactory")
//    public RedisConnectionFactory defaultRedisConnectionFactory() {
//        return createConnectionFactory(0);
//    }
//
//    // 캐시용 Redis 연결 팩토리 (DB 1)
//    // 상품 할인율 등 캐시 데이터
//    @Bean("cacheRedisConnectionFactory")
//    public RedisConnectionFactory cacheRedisConnectionFactory() {
//        return createConnectionFactory(1);
//    }
//
//    // 세션용 Redis 연결 팩토리 (DB 2)
//    // 세션, JWT 토큰 관리
//    @Bean("sessionRedisConnectionFactory")
//    public RedisConnectionFactory sessionRedisConnectionFactory() {
//        return createConnectionFactory(2);
//    }
//
//    // 알림용 Redis 연결 팩토리 (DB 3)
//    // SSE 연결, 알림 관리
//    @Bean("notificationRedisConnectionFactory")
//    public RedisConnectionFactory notificationRedisConnectionFactory() {
//        return createConnectionFactory(3);
//    }
//
//    // 검색용 Redis 연결 팩토리 (DB 4)
//    // 검색 기록, 인기 검색어
//    @Bean("searchRedisConnectionFactory")
//    public RedisConnectionFactory searchRedisConnectionFactory() {
//        return createConnectionFactory(4);
//    }
//
//    // 장바구니용 Redis 연결 팩토리 (DB 5)
//    // 장바구니 데이터
//    @Bean("cartRedisConnectionFactory")
//    public RedisConnectionFactory cartRedisConnectionFactory() {
//        return createConnectionFactory(5);
//    }
//
//    // 채팅용 Redis 연결 팩토리 (DB 6)
//    // 채팅 활성 상태
//    @Bean("chatRedisConnectionFactory")
//    public RedisConnectionFactory chatRedisConnectionFactory() {
//        return createConnectionFactory(6);
//    }
//
//    private RedisConnectionFactory createConnectionFactory(int database) {
//        LettuceConnectionFactory factory = new LettuceConnectionFactory();
//        factory.setHostName(host);
//        factory.setPort(port);
//        factory.setDatabase(database);
//        factory.afterPropertiesSet();
//        return factory;
//    }
//
//    // 기본 RedisTemplate (DB 0)
//    @Primary
//    @Bean("defaultRedisTemplate")
//    public RedisTemplate<String, Object> defaultRedisTemplate(
//            @Qualifier("defaultRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        return createRedisTemplate(connectionFactory);
//    }
//
//    // 캐시용 RedisTemplate (DB 1)
//    @Bean("cacheRedisTemplate")
//    public RedisTemplate<String, Object> cacheRedisTemplate(
//            @Qualifier("cacheRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        return createRedisTemplate(connectionFactory);
//    }
//
//    // 세션용 RedisTemplate (DB 2)
//    @Bean("sessionRedisTemplate")
//    public RedisTemplate<String, Object> sessionRedisTemplate(
//            @Qualifier("sessionRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        return createRedisTemplate(connectionFactory);
//    }
//
//    // 알림용 RedisTemplate (DB 3)
//    @Bean("notificationRedisTemplate")
//    public RedisTemplate<String, Object> notificationRedisTemplate(
//            @Qualifier("notificationRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        return createRedisTemplate(connectionFactory);
//    }
//
//    // 검색용 RedisTemplate (DB 4)
//    @Bean("searchRedisTemplate")
//    public RedisTemplate<String, Object> searchRedisTemplate(
//            @Qualifier("searchRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        return createRedisTemplate(connectionFactory);
//    }
//
//    // 장바구니용 RedisTemplate (DB 5)
//    @Bean("cartRedisTemplate")
//    public RedisTemplate<String, Object> cartRedisTemplate(
//            @Qualifier("cartRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        return createRedisTemplate(connectionFactory);
//    }
//
//    // 채팅용 RedisTemplate (DB 6)
//    @Bean("chatRedisTemplate")
//    public RedisTemplate<String, Object> chatRedisTemplate(
//            @Qualifier("chatRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        return createRedisTemplate(connectionFactory);
//    }
//
//    // String RedisTemplate들
//    @Bean("defaultStringRedisTemplate")
//    public StringRedisTemplate defaultStringRedisTemplate(
//            @Qualifier("defaultRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        StringRedisTemplate template = new StringRedisTemplate();
//        template.setConnectionFactory(connectionFactory);
//        return template;
//    }
//
//    @Bean("searchStringRedisTemplate")
//    public StringRedisTemplate searchStringRedisTemplate(
//            @Qualifier("searchRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
//        StringRedisTemplate template = new StringRedisTemplate();
//        template.setConnectionFactory(connectionFactory);
//        return template;
//    }
//
//    // RedisTemplate 생성 헬퍼 메서드
//    private RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(new StringRedisSerializer());
//        template.afterPropertiesSet();
//        return template;
//    }
//}
