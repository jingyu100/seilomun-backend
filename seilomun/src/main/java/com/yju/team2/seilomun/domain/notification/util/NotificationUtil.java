package com.yju.team2.seilomun.domain.notification.util;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import com.yju.team2.seilomun.domain.product.entity.Product;

public class NotificationUtil {

    // 즐겨찾기한 가게에서 상품 등록 했을 때
    public static Notification createNewProductNotification(Product product, Long customerId, Long sellerId) {

        return Notification.builder()
                .content(String.format("%s 매장에 새로운 상품 '%s'이(가) 등록되었습니다.",
                        product.getSeller().getStoreName(), product.getName()))
                .isVisible('Y')
                .recipientType('C')
                .recipientId(customerId)
                .senderType('S')
                .senderId(sellerId)
                .isRead('N')
                .build();

    }
}
