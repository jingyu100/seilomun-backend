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
public class OrderDeclinedEvent implements NotificationEvent {
    private Order order;
    private String eventId;

    @Override
    public NotificationType getType() {
        return NotificationType.ORDER_DECLINED;
    }

    @Override
    public Long getSenderId() {
        return order.getSeller().getId();
    }

    @Override
    public Character getSenderType() {
        return 'S'; // Seller
    }

    @Override
    public Object getEventData() {
        return order;
    }

    @Override
    public String getEventId() {
        return eventId != null ? eventId : "ORDER_DECLINED_" + order.getId();
    }
}