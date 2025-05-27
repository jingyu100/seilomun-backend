package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationStrategyFactory {

    private final NewProductNotificationStrategy newProductStrategy;

    private final Map<NotificationType, NotificationStrategy> strategies = new HashMap<>();

    public NotificationStrategyFactory(
            NewProductNotificationStrategy newProductStrategy) {
        this.newProductStrategy = newProductStrategy;

        // 전략 매핑
        strategies.put(NotificationType.NEW_PRODUCT, newProductStrategy);
        // 필요에 따라 추가 전략들을 매핑
    }

    public NotificationStrategy getStrategy(NotificationType type) {
        NotificationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for notification type: " + type);
        }
        return strategy;
    }
}