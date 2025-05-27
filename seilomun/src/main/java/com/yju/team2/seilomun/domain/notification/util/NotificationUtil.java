package com.yju.team2.seilomun.domain.notification.util;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.product.entity.Product;

public class NotificationUtil {

    public static Notification createNotification(NotificationEvent event, Long recipientId, Character recipientType) {
        String content = generateContent(event);

        return Notification.builder()
                .content(content)
                .isVisible('Y')
                .recipientType(recipientType)
                .recipientId(recipientId)
                .senderType(event.getSenderType())
                .senderId(event.getSenderId())
                .isRead('N')
                .build();
    }

    // 알림 내용 생성
    private static String generateContent(NotificationEvent event) {

        switch (event.getType()) {

            // 소비자에게 보내는 알림
            case NEW_PRODUCT:
                Product product = (Product) event.getEventData();
                return String.format("%s 매장에 새로운 상품 '%s'이(가) 등록되었습니다.",
                        product.getSeller().getStoreName(), product.getName());
            case ORDER_ACCEPTED:
            case ORDER_DECLINED:
            case ORDER_REFUND_ACCEPTED:
            case ORDER_REFUND_DECLINED:
            case LIKE_PRODUCT_STATUS_CHANGED:
            case CART_PRODUCT_STATUS_CHANGED:

                // 판매자에게 보내는 알림
            case ORDER_OFFERED:
            case ORDER_REFUND:
            case PRODUCT_STATUS_CHANGED:
            case REVIEW_WRITTEN:
            case PAYMENT_COMPLETED:

            default:
                return "새로운 알림이 있습니다.";
        }
    }
}
