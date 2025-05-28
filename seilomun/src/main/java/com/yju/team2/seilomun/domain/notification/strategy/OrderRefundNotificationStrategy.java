package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.order.entity.Refund;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrderRefundNotificationStrategy implements NotificationStrategy {

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Refund refund = (Refund) event.getEventData();
        return Collections.singletonList(refund.getPayment().getOrder().getSeller().getId());
    }

    @Override
    public Character getRecipientType() {
        return 'S'; // Seller
    }
}