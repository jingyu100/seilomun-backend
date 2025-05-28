package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.customer.entity.Wish;
import com.yju.team2.seilomun.domain.customer.repository.WishRepository;
import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LikeProductStatusChangedNotificationStrategy implements NotificationStrategy {

    private final WishRepository wishRepository;

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Product product = (Product) event.getEventData();

        // 해당 상품을 좋아요한 모든 고객들에게 알림
        List<Wish> wishes = wishRepository.findByProduct(product);

        return wishes.stream()
                .map(wish -> wish.getCustomer().getId())
                .collect(Collectors.toList());
    }

    @Override
    public Character getRecipientType() {
        return 'C'; // Customer
    }
}
