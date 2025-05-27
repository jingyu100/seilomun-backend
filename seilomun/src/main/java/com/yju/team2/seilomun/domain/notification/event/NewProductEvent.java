package com.yju.team2.seilomun.domain.notification.event;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;
import com.yju.team2.seilomun.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewProductEvent implements NotificationEvent {

    private Product product;
    private String eventId;

    @Override
    public NotificationType getType() {
        return NotificationType.NEW_PRODUCT;
    }

    @Override
    public Long getSenderId() {
        return product.getSeller().getId();
    }

    @Override
    public Character getSenderType() {
        return 'S'; // Seller
    }

    @Override
    public Object getEventData() {
        return product;
    }

    @Override
    public String getEventId() {
        return eventId != null ? eventId : "PRODUCT_" + product.getId();
    }
}