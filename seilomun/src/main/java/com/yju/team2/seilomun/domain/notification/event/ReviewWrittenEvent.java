package com.yju.team2.seilomun.domain.notification.event;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;
import com.yju.team2.seilomun.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewWrittenEvent implements NotificationEvent {

    private Review review;
    private String eventId;

    @Override
    public NotificationType getType() {
        return NotificationType.REVIEW_WRITTEN;
    }

    @Override
    public Long getSenderId() {
        return review.getOrder().getCustomer().getId();
    }

    @Override
    public Character getSenderType() {
        return 'C'; // Customer
    }

    @Override
    public Object getEventData() {
        return review;
    }

    @Override
    public String getEventId() {
        return eventId != null ? eventId : "REVIEW_" + review.getId();
    }
}