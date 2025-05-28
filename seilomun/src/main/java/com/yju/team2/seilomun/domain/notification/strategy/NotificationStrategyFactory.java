package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
//@RequiredArgsConstructor
public class NotificationStrategyFactory {

    private final NewProductNotificationStrategy newProductStrategy;
    private final OrderAcceptedNotificationStrategy orderAcceptedStrategy;
    private final OrderDeclinedNotificationStrategy orderDeclinedStrategy;
    private final OrderRefundAcceptedNotificationStrategy orderRefundAcceptedStrategy;
    private final OrderRefundDeclinedNotificationStrategy orderRefundDeclinedStrategy;
    private final LikeProductStatusChangedNotificationStrategy likeProductStatusChangedStrategy;
    private final CartProductStatusChangedNotificationStrategy cartProductStatusChangedStrategy;
    private final OrderOfferedNotificationStrategy orderOfferedStrategy;
    private final OrderRefundNotificationStrategy orderRefundStrategy;
    private final ProductStatusChangedNotificationStrategy productStatusChangedStrategy;
    private final ReviewWrittenNotificationStrategy reviewWrittenStrategy;
    private final PaymentCompletedNotificationStrategy paymentCompletedStrategy;

    private final Map<NotificationType, NotificationStrategy> strategies = new HashMap<>();

    public NotificationStrategyFactory(
            NewProductNotificationStrategy newProductStrategy,
            OrderAcceptedNotificationStrategy orderAcceptedStrategy,
            OrderDeclinedNotificationStrategy orderDeclinedStrategy,
            OrderRefundAcceptedNotificationStrategy orderRefundAcceptedStrategy,
            OrderRefundDeclinedNotificationStrategy orderRefundDeclinedStrategy,
            LikeProductStatusChangedNotificationStrategy likeProductStatusChangedStrategy,
            CartProductStatusChangedNotificationStrategy cartProductStatusChangedStrategy,
            OrderOfferedNotificationStrategy orderOfferedStrategy,
            OrderRefundNotificationStrategy orderRefundStrategy,
            ProductStatusChangedNotificationStrategy productStatusChangedStrategy,
            ReviewWrittenNotificationStrategy reviewWrittenStrategy,
            PaymentCompletedNotificationStrategy paymentCompletedStrategy) {

        this.newProductStrategy = newProductStrategy;
        this.orderAcceptedStrategy = orderAcceptedStrategy;
        this.orderDeclinedStrategy = orderDeclinedStrategy;
        this.orderRefundAcceptedStrategy = orderRefundAcceptedStrategy;
        this.orderRefundDeclinedStrategy = orderRefundDeclinedStrategy;
        this.likeProductStatusChangedStrategy = likeProductStatusChangedStrategy;
        this.cartProductStatusChangedStrategy = cartProductStatusChangedStrategy;
        this.orderOfferedStrategy = orderOfferedStrategy;
        this.orderRefundStrategy = orderRefundStrategy;
        this.productStatusChangedStrategy = productStatusChangedStrategy;
        this.reviewWrittenStrategy = reviewWrittenStrategy;
        this.paymentCompletedStrategy = paymentCompletedStrategy;

        initializeStrategies();
    }

    private void initializeStrategies() {
        // 소비자에게 보내는 알림들
        strategies.put(NotificationType.NEW_PRODUCT, newProductStrategy);
        strategies.put(NotificationType.ORDER_ACCEPTED, orderAcceptedStrategy);
        strategies.put(NotificationType.ORDER_DECLINED, orderDeclinedStrategy);
        strategies.put(NotificationType.ORDER_REFUND_ACCEPTED, orderRefundAcceptedStrategy);
        strategies.put(NotificationType.ORDER_REFUND_DECLINED, orderRefundDeclinedStrategy);
        strategies.put(NotificationType.LIKE_PRODUCT_STATUS_CHANGED, likeProductStatusChangedStrategy);
        strategies.put(NotificationType.CART_PRODUCT_STATUS_CHANGED, cartProductStatusChangedStrategy);

        // 판매자에게 보내는 알림들
        strategies.put(NotificationType.ORDER_OFFERED, orderOfferedStrategy);
        strategies.put(NotificationType.ORDER_REFUND, orderRefundStrategy);
        strategies.put(NotificationType.PRODUCT_STATUS_CHANGED, productStatusChangedStrategy);
        strategies.put(NotificationType.REVIEW_WRITTEN, reviewWrittenStrategy);
        strategies.put(NotificationType.PAYMENT_COMPLETED, paymentCompletedStrategy);
    }

    public NotificationStrategy getStrategy(NotificationType type) {
        NotificationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for notification type: " + type);
        }
        return strategy;
    }

}