package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.customer.entity.Favorite;
import com.yju.team2.seilomun.domain.customer.repository.FavoriteRepository;
import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NewProductNotificationStrategy implements NotificationStrategy {

    private final FavoriteRepository favoriteRepository;

    @Override
    public List<Long> getRecipients(NotificationEvent event) {
        Product product = (Product) event.getEventData();
        Long sellerId = product.getSeller().getId();

        // 해당 판매자를 즐겨찾기한 모든 고객 조회
        List<Favorite> favorites = favoriteRepository.findBySellerId(sellerId);

        return favorites.stream()
                .map(favorite -> favorite.getCustomer().getId())
                .collect(Collectors.toList());
    }

    @Override
    public Character getRecipientType() {
        return 'C'; // Customer
    }

}