package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.order.entity.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrderOfferedNotificationStrategy implements NotificationStrategy {

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Order order = (Order) event.getEventData();
        return Collections.singletonList(order.getSeller().getId());
    }

    @Override
    public Character getRecipientType() {
        return 'S'; // Seller
    }
}