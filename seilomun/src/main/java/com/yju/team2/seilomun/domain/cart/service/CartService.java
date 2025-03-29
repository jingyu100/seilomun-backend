package com.yju.team2.seilomun.domain.cart.service;

import com.yju.team2.seilomun.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final String CART_KEY = "cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductService productService;

    // 장바구니 상품 추가
    public int addToCart(Long userId, Long productId, Integer quantity) {

        // 상품 존재 여부 확인
        if (!productService.existsById(productId)) {
            throw new RuntimeException("존재하지 않는 상품입니다: " + productId);
        }

        String cartKey = getCartKey(userId);

        try {
            // 현재 장바구니에 있는 상품 수량 확인
            Integer currentQuantity = 0;
            Object currentValue = redisTemplate.opsForHash().get(cartKey, productId.toString());

            if (currentValue != null) {
                currentQuantity = parseQuantity(currentValue);
            }

            // 새 수량 계산
            int newQuantity = currentQuantity + quantity;

            // 장바구니에 상품 추가/업데이트
            redisTemplate.opsForHash().put(cartKey, productId.toString(), String.valueOf(newQuantity));

            log.info("상품이 장바구니에 추가되었습니다. userId={}, productId={}, quantity={}, totalQuantity={}",
                    userId, productId, quantity, newQuantity);

            return newQuantity;
        } catch (Exception e) {
            log.error("장바구니에 상품 추가 중 오류 발생: userId={}, productId={}, quantity={}",
                    userId, productId, quantity, e);
            throw new RuntimeException("장바구니에 상품을 추가하는 중 오류가 발생했습니다", e);
        }
    }

    // 장바구니 상품 수량 변경
    public void updateCartItemQuantity(Long userId, Long productId, Integer quantity) {

        if (quantity <= 0) {
            removeFromCart(userId, productId);
            return;
        }

        String cartKey = getCartKey(userId);

        try {
            if (!productService.existsById(productId)) {
                throw new RuntimeException("존재하지 않는 상품입니다: " + productId);
            }

            redisTemplate.opsForHash().put(cartKey, productId.toString(), quantity.toString());
            log.info("장바구니 상품 수량이 업데이트되었습니다. userId={}, productId={}, quantity={}",
                    userId, productId, quantity);
        } catch (Exception e) {
            log.error("장바구니 상품 수량 업데이트 중 오류 발생: userId={}, productId={}, quantity={}",
                    userId, productId, quantity, e);
            throw new RuntimeException("장바구니 상품 수량 업데이트 중 오류가 발생했습니다", e);
        }
    }

    // 장바구니 상품 조회
    public Map<Long, Integer> getCartItems(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }

        String cartKey = getCartKey(userId);

        try {
            if (!redisTemplate.hasKey(cartKey)) {
                return new HashMap<>();
            }

            Map<Object, Object> rawItems = redisTemplate.opsForHash().entries(cartKey);

            // 타입 변환하여 반환
            return rawItems.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> Long.valueOf(entry.getKey().toString()),
                            entry -> parseQuantity(entry.getValue())
                    ));
        } catch (Exception e) {
            log.error("장바구니 조회 중 오류 발생: userId={}", userId, e);
            throw new RuntimeException("장바구니 조회 중 오류가 발생했습니다", e);
        }
    }

    // 장바구니 특정 상품 삭제
//    public void removeFromCart(Long userId, Long productId) {
//
//        String cartKey = getCartKey(userId);
//
//        try {
//            if (redisTemplate.hasKey(cartKey) &&
//                    redisTemplate.opsForHash().hasKey(cartKey, productId.toString())) {
//
//                Long deleted = redisTemplate.opsForHash().delete(cartKey, productId.toString());
//
//                if (deleted > 0) {
//                    log.info("상품이 장바구니에서 제거되었습니다. userId={}, productId={}", userId, productId);
//                }
//            }
//        } catch (Exception e) {
//            log.error("장바구니에서 상품 제거 중 오류 발생: userId={}, productId={}", userId, productId, e);
//            throw new RuntimeException("장바구니에서 상품 제거 중 오류가 발생했습니다", e);
//        }
//    }
    public boolean removeFromCart(Long userId, Long productId) {
        String cartKey = getCartKey(userId);

        try {
            if (!redisTemplate.hasKey(cartKey)) {
                log.info("장바구니가 존재하지 않습니다: userId={}", userId);
                return false;
            }

            if (!redisTemplate.opsForHash().hasKey(cartKey, productId.toString())) {
                log.info("장바구니에 해당 상품이 존재하지 않습니다: userId={}, productId={}", userId, productId);
                return false;
            }

            Long deleted = redisTemplate.opsForHash().delete(cartKey, productId.toString());

            if (deleted > 0) {
                log.info("상품이 장바구니에서 제거되었습니다: userId={}, productId={}", userId, productId);
                return true;
            } else {
                log.warn("상품 제거 시도 실패: userId={}, productId={}", userId, productId);
                return false;
            }
        } catch (Exception e) {
            log.error("장바구니에서 상품 제거 중 오류 발생: userId={}, productId={}", userId, productId, e);
            throw new RuntimeException("장바구니에서 상품 제거 중 오류가 발생했습니다", e);
        }
    }

    // 장바구니 초기화
    public void clearCart(Long userId) {

        String key = getCartKey(userId);

        try {
            if (redisTemplate.hasKey(key)) {
                redisTemplate.delete(key);
                log.info("장바구니가 비워졌습니다. userId={}", userId);
            }
        } catch (Exception e) {
            log.error("장바구니 비우기 중 오류 발생: userId={}", userId, e);
            throw new RuntimeException("장바구니 비우기 중 오류가 발생했습니다", e);
        }
    }

    private static String getCartKey(Long userId) {
        return CART_KEY + userId;
    }

    // 수량 파싱 헬퍼 메서드
    private int parseQuantity(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        return 0;
    }
}