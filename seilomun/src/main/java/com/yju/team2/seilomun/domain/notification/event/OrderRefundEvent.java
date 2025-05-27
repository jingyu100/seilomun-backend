package com.yju.team2.seilomun.domain.notification.event;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;
import com.yju.team2.seilomun.domain.order.entity.Refund;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRefundEvent implements NotificationEvent {

    private Refund refund;
    private String eventId;

    @Override
    public NotificationType getType() {
        return NotificationType.ORDER_REFUND;
    }

    @Override
    public Long getSenderId() {
        return refund.getPayment().getOrder().getCustomer().getId();
    }

    @Override
    public Character getSenderType() {
        return 'C'; // Customer
    }

    @Override
    public Object getEventData() {
        return refund;
    }

    @Override
    public String getEventId() {
        return eventId != null ? eventId : "REFUND_" + refund.getId();
    }
}
