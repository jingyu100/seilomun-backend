package com.yju.team2.seilomun.domain.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 장바구니 상품 추가
    public void addToCart(Long userId, Long productId, Integer quantity) {
        String key = "cart:" + userId;

        // 장바구니에서 해당 상품의 수량을 가져옴
        Integer existingQuantity = (Integer) redisTemplate.opsForHash().get(key, productId.toString());

        // 상품이 이미 장바구니에 존재하는지 확인하고, 수량을 업데이트
        int newQuantity = (existingQuantity == null) ? quantity : existingQuantity + quantity;

        // 장바구니에 상품 추가 또는 수량 업데이트 - String으로 변환하여 저장
        redisTemplate.opsForHash().put(key, productId.toString(), String.valueOf(newQuantity));
    }

    // 장바구니 상품 조회
    public String getCartItems(Long userId) {
        String key = "cart:" + userId;

        // 장바구니가 존재하면 해당 내용을 반환, 없으면 메시지 반환
        if (redisTemplate.hasKey(key)) {
            Map<Object, Object> cartItems = redisTemplate.opsForHash().entries(key);
            return cartItems.toString(); // Map을 문자열로 변환하여 반환
        }

        return "장바구니에 상품이 없습니다.";
    }

    // 장바구니 특정 상품 삭제
    public void removeFromCart(Long userId, Long productId) {
        String key = "cart:" + userId;

        // 장바구니가 존재하면 해당 상품 삭제
        if (redisTemplate.hasKey(key) && redisTemplate.opsForHash().hasKey(key, productId.toString())) {
            redisTemplate.opsForHash().delete(key, productId.toString());
        }
    }

    // 장바구니 초기화
    public void clearCart(Long userId) {
        String key = "cart:" + userId;

        // 장바구니가 존재하면 해당 내용을 삭제
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
        }
    }
}