package com.yju.team2.seilomun.domain.notification.event;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;
import com.yju.team2.seilomun.domain.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOfferedEvent implements NotificationEvent {

    private Order order;
    private String eventId;

    @Override
    public NotificationType getType() {
        return NotificationType.ORDER_OFFERED;
    }

    @Override
    public Long getSenderId() {
        return order.getCustomer().getId();
    }

    @Override
    public Character getSenderType() {
        return 'C'; // Customer
    }

    @Override
    public Object getEventData() {
        return order;
    }

    @Override
    public String getEventId() {
        return eventId != null ? eventId : "ORDER_OFFERED_" + order.getOrId();
    }
}