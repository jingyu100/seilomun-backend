package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.review.entity.Review;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ReviewWrittenNotificationStrategy implements NotificationStrategy {

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Review review = (Review) event.getEventData();
        return Collections.singletonList(review.getOrder().getSeller().getId());
    }

    @Override
    public Character getRecipientType() {
        return 'S'; // Seller
    }
}