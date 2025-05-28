package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.product.entity.Product;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductStatusChangedNotificationStrategy implements NotificationStrategy {

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Product product = (Product) event.getEventData();
        return Collections.singletonList(product.getSeller().getId());
    }

    @Override
    public Character getRecipientType() {
        return 'S'; // Seller
    }
}