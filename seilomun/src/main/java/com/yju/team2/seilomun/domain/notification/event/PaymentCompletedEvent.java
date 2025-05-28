package com.yju.team2.seilomun.domain.notification.event;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;
import com.yju.team2.seilomun.domain.order.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent implements NotificationEvent {

    private Payment payment;
    private String eventId;

    @Override
    public NotificationType getType() {
        return NotificationType.PAYMENT_COMPLETED;
    }

    @Override
    public Long getSenderId() {
        return payment.getOrder().getCustomer().getId();
    }

    @Override
    public Character getSenderType() {
        return 'C'; // Customer
    }

    @Override
    public Object getEventData() {
        return payment;
    }

    @Override
    public String getEventId() {
        return eventId != null ? eventId : "PAYMENT_" + payment.getId();
    }
}