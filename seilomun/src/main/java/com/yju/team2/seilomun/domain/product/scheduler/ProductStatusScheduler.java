package com.yju.team2.seilomun.domain.product.scheduler;

import com.yju.team2.seilomun.domain.notification.event.CartProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.event.LikeProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.event.ProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.service.NotificationService;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductStatusScheduler {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    // 매일 새벽 1시에 유통기한 확인
    @Scheduled(cron = "0 0 1 * * ?")
    public void updatedExpiredProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> expiredProducts = productRepository
                .findByExpiryDateBeforeAndStatusNot(now, 'X');

        for (Product expiredProduct : expiredProducts) {
            Character oldStatus = expiredProduct.getStatus();
            expiredProduct.updateStatus('X');
            productRepository.save(expiredProduct);

            // 알림 전송
            sendProductStatusChangeNotifications(expiredProduct, oldStatus, 'X');
        }
    }

//    // 매시간 임박 상품 확인 (예: 3일 전)
//    @Scheduled(cron = "0 0 * * * ?")
//    public void updateExpiringProducts() {
//        LocalDateTime threeDaysLater = LocalDateTime.now().plusDays(3);
//        List<Product> expiringProducts = productRepository
//                .findByExpiryDateBeforeAndStatus(threeDaysLater, '1'); // 판매중인 상품만
//
//        for (Product product : expiringProducts) {
//            product.updateStatus('T'); // 'T' = 임박특가
//            productRepository.save(product);
//
//            // 알림 발생
//            sendProductStatusChangeNotifications(product, '1', 'T');
//        }
//    }

    // 상태 변경 알림 메서드
    private void sendProductStatusChangeNotifications(Product product, Character oldStatus, Character newStatus) {
        try {
            // 1. 판매자에게 알림
            ProductStatusChangedEvent sellerEvent = ProductStatusChangedEvent.builder()
                    .product(product)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .eventId("PRODUCT_STATUS_" + product.getId())
                    .build();
            notificationService.processNotification(sellerEvent);

            // 2. 좋아요한 고객들에게 알림
            LikeProductStatusChangedEvent likeEvent = LikeProductStatusChangedEvent.builder()
                    .product(product)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .eventId("LIKE_PRODUCT_STATUS_" + product.getId())
                    .build();
            notificationService.processNotification(likeEvent);

            // 3. 장바구니에 담은 고객들에게 알림
            CartProductStatusChangedEvent cartEvent = CartProductStatusChangedEvent.builder()
                    .product(product)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .eventId("CART_PRODUCT_STATUS_" + product.getId())
                    .build();
            notificationService.processNotification(cartEvent);

        } catch (Exception e) {
            log.error("상품 상태 변경 알림 전송 실패: productId={}", product.getId(), e);
        }
    }
}
