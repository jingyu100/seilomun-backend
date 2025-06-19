package com.yju.team2.seilomun.domain.product.scheduler;

import com.yju.team2.seilomun.domain.notification.event.CartProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.event.LikeProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.event.ProductStatusChangedEvent;
import com.yju.team2.seilomun.domain.notification.service.NotificationService;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.service.ProductIndexService;
import com.yju.team2.seilomun.domain.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductStatusScheduler {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final ProductIndexService productIndexService;
    private final ProductSearchRepository productSearchRepository;

//    // 매분마다 ES와 DB의 product data 동기화
//    @Scheduled(cron = "0 * * * * ?")
//    @Transactional(readOnly = true)
//    public void syncProductDataWithElasticsearch() {
//        try {
//            List<Product> allProducts = productRepository.findAll();
//            int syncCount = 0;
//            int errorCount = 0;
//
//            for (Product product : allProducts) {
//                try {
//                    // DB의 최신 상품 정보로 ES 인덱스 업데이트
//                    ProductDocument productDocument = ProductDocument.from(product);
//
//                    // ES에서 기존 문서 조회
//                    Optional<ProductDocument> existingDoc = productSearchRepository.findById(String.valueOf(product.getId()));
//
//                    // ES에 문서가 없거나 내용이 다른 경우에만 업데이트
//                    boolean needsUpdate = true;
//                    if (existingDoc.isPresent()) {
//                        needsUpdate = !isProductDocumentEqual(existingDoc.get(), productDocument);
//                    }
//
//                    if (needsUpdate) {
//                        productSearchRepository.save(productDocument);
//                        syncCount++;
//                        log.debug("ES 동기화 완료: productId={}", product.getId());
//                    }
//
//                } catch (Exception e) {
//                    errorCount++;
//                    log.error("ES 동기화 실패: productId={}", product.getId(), e);
//                }
//            }
//
//            if (syncCount > 0 || errorCount > 0) {
//                log.info("ES-DB 동기화 완료: 성공={}, 실패={}, 전체={}", syncCount, errorCount, allProducts.size());
//            }
//
//        } catch (Exception e) {
//            log.error("ES-DB 전체 동기화 작업 실패", e);
//        }
//    }

    // 매일 새벽 1시에 유통기한 확인 및 Elasticsearch 동기화
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void updateExpiredProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> expiredProducts = productRepository
                .findByExpiryDateBeforeAndStatusNot(now, 'X');

        int updatedCount = 0;

        for (Product expiredProduct : expiredProducts) {
            Character oldStatus = expiredProduct.getStatus();
            expiredProduct.updateStatus('X');
            productRepository.save(expiredProduct);

            // Elasticsearch 인덱스 업데이트
            try {
                ProductDocument productDocument = ProductDocument.from(expiredProduct);
                productSearchRepository.save(productDocument);
                log.debug("Elasticsearch 인덱스 업데이트 완료: productId={}", expiredProduct.getId());
            } catch (Exception e) {
                log.error("Elasticsearch 인덱스 업데이트 실패: productId={}", expiredProduct.getId(), e);
                // 인덱스 업데이트 실패해도 상태 변경은 유지
            }

            // 알림 전송
            sendProductStatusChangeNotifications(expiredProduct, oldStatus, 'X');
            updatedCount++;
        }

        if (updatedCount > 0) {
            log.info("만료된 상품 {} 개의 상태를 'X'로 업데이트하고 Elasticsearch 인덱스를 동기화했습니다.", updatedCount);
        }
    }

    // 매시간 임박 상품 확인 및 Elasticsearch 동기화 (예: 3일 전)
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void updateExpiringProducts() {
        LocalDateTime threeDaysLater = LocalDateTime.now().plusDays(3);
        List<Product> expiringProducts = productRepository
                .findByExpiryDateBeforeAndStatus(threeDaysLater, '1'); // 판매중인 상품만

        int updatedCount = 0;

        for (Product product : expiringProducts) {
            Character oldStatus = product.getStatus();
            product.updateStatus('T'); // 'T' = 임박특가
            productRepository.save(product);

            // Elasticsearch 인덱스 업데이트
            try {
                ProductDocument productDocument = ProductDocument.from(product);
                productSearchRepository.save(productDocument);
                log.debug("Elasticsearch 인덱스 업데이트 완료: productId={}", product.getId());
            } catch (Exception e) {
                log.error("Elasticsearch 인덱스 업데이트 실패: productId={}", product.getId(), e);
                // 인덱스 업데이트 실패해도 상태 변경은 유지
            }

            // 알림 발생
            sendProductStatusChangeNotifications(product, oldStatus, 'T');
            updatedCount++;
        }

        if (updatedCount > 0) {
            log.info("임박 상품 {} 개의 상태를 'T'로 업데이트하고 Elasticsearch 인덱스를 동기화했습니다.", updatedCount);
        }
    }

    // 매시간 재고 부족 상품 확인 (재고가 0인 상품을 'E' 상태로 변경)
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void updateOutOfStockProducts() {
        List<Product> outOfStockProducts = productRepository
                .findByStockQuantityAndStatusNot(0, 'E'); // 재고가 0이고 상태가 'E'가 아닌 상품들

        int updatedCount = 0;

        for (Product product : outOfStockProducts) {
            Character oldStatus = product.getStatus();
            product.updateStatus('E'); // 'E' = 품절
            productRepository.save(product);

            // Elasticsearch 인덱스 업데이트
            try {
                ProductDocument productDocument = ProductDocument.from(product);
                productSearchRepository.save(productDocument);
                log.debug("Elasticsearch 인덱스 업데이트 완료: productId={}", product.getId());
            } catch (Exception e) {
                log.error("Elasticsearch 인덱스 업데이트 실패: productId={}", product.getId(), e);
                // 인덱스 업데이트 실패해도 상태 변경은 유지
            }

            // 알림 발생
            sendProductStatusChangeNotifications(product, oldStatus, 'E');
            updatedCount++;
        }

        if (updatedCount > 0) {
            log.info("품절 상품 {} 개의 상태를 'E'로 업데이트하고 Elasticsearch 인덱스를 동기화했습니다.", updatedCount);
        }
    }

    // 매시간 재입고 상품 확인 (품절 상태였다가 재고가 다시 생긴 상품들)
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void updateRestockedProducts() {
        List<Product> restockedProducts = productRepository
                .findByStockQuantityGreaterThanAndStatus(0, 'E'); // 재고가 0보다 크고 상태가 'E'인 상품들

        int updatedCount = 0;

        for (Product product : restockedProducts) {
            Character oldStatus = product.getStatus();

            // 유통기한을 확인하여 적절한 상태로 복구
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeDaysLater = now.plusDays(3);

            Character newStatus;
            if (product.getExpiryDate().isBefore(now)) {
                newStatus = 'X'; // 유통기한 만료
            } else if (product.getExpiryDate().isBefore(threeDaysLater)) {
                newStatus = 'T'; // 임박특가
            } else {
                newStatus = '1'; // 정상 판매
            }

            product.updateStatus(newStatus);
            productRepository.save(product);

            // Elasticsearch 인덱스 업데이트
            try {
                ProductDocument productDocument = ProductDocument.from(product);
                productSearchRepository.save(productDocument);
                log.debug("Elasticsearch 인덱스 업데이트 완료: productId={}", product.getId());
            } catch (Exception e) {
                log.error("Elasticsearch 인덱스 업데이트 실패: productId={}", product.getId(), e);
                // 인덱스 업데이트 실패해도 상태 변경은 유지
            }

            // 알림 발생
            sendProductStatusChangeNotifications(product, oldStatus, newStatus);
            updatedCount++;
        }

        if (updatedCount > 0) {
            log.info("재입고 상품 {} 개의 상태를 복구하고 Elasticsearch 인덱스를 동기화했습니다.", updatedCount);
        }
    }

    // ProductDocument 동등성 비교 메서드
    private boolean isProductDocumentEqual(ProductDocument existing, ProductDocument current) {
        if (existing == null || current == null) {
            return false;
        }

        return existing.getId().equals(current.getId()) &&
                existing.getName().equals(current.getName()) &&
                existing.getStatus().equals(current.getStatus()) &&
                existing.getOriginalPrice().equals(current.getOriginalPrice()) &&
                existing.getStockQuantity().equals(current.getStockQuantity()) &&
                existing.getExpiryDate().equals(current.getExpiryDate()) &&
                existing.getCategoryId().equals(current.getCategoryId()) &&
                existing.getDescription().equals(current.getDescription()) &&
                existing.getSellerId().equals(current.getSellerId()) &&
                existing.getCreatedAt().equals(current.getCreatedAt()) &&
                existing.getExpiryDate().equals(current.getExpiryDate());
    }

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
