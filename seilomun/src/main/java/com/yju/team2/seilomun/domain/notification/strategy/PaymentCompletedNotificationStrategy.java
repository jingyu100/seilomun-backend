package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.order.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PaymentCompletedNotificationStrategy implements NotificationStrategy {

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Payment payment = (Payment) event.getEventData();
        return Collections.singletonList(payment.getOrder().getSeller().getId());
    }

    @Override
    public Character getRecipientType() {
        return 'S'; // Seller
    }
}