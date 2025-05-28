package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.cart.service.CartService;
import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CartProductStatusChangedNotificationStrategy implements NotificationStrategy {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CART_KEY = "cart:";

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Product product = (Product) event.getEventData();
        List<Long> customerIds = new ArrayList<>();

        // Redis에서 해당 상품을 장바구니에 담은 모든 사용자 찾기
        Set<String> cartKeys = redisTemplate.keys(CART_KEY + "*");

        if (cartKeys != null) {
            for (String cartKey : cartKeys) {
                // cart:userId 형태에서 userId 추출
                String userId = cartKey.replace(CART_KEY, "");

                // 해당 사용자의 장바구니에 이 상품이 있는지 확인
                Boolean hasProduct = redisTemplate.opsForHash().hasKey(cartKey, product.getId().toString());

                if (Boolean.TRUE.equals(hasProduct)) {
                    try {
                        customerIds.add(Long.parseLong(userId));
                    } catch (NumberFormatException e) {
                        // 잘못된 userId 형태는 무시
                    }
                }
            }
        }

        return customerIds;
    }

    @Override
    public Character getRecipientType() {
        return 'C'; // Customer
    }
}