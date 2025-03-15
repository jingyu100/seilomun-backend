//package com.yju.team2.seilomun.domain.product.repository;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.util.concurrent.TimeUnit;
//
//@Repository
//@RequiredArgsConstructor
//public class CurrentDiscountRateRepository {
//
//    private final RedisTemplate<String,Integer> redisTemplate;
//
//    //할인율 조회 메서드
//    public Integer getDiscounRate(Long id)
//    {
//        return redisTemplate.opsForValue().get("discountRate:" + id);
//    }
//
//    //할인율 저장 메서드
//    public void saveCurrentDiscountRate(Long id, Integer CurrentDiscountRate)
//    {
//        redisTemplate.opsForValue().set("discountRate:" + id, CurrentDiscountRate,30, TimeUnit.MINUTES);
//    }
//}
