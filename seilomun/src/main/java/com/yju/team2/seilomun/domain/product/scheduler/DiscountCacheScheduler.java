package com.yju.team2.seilomun.domain.product.scheduler;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.service.ProductDiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscountCacheScheduler {

    private final ProductDiscountService discountService;
    private final ProductRepository productRepository;

    /**
     * 매시간 만료 임박 상품들의 할인 캐시 갱신
     */
    @Scheduled(cron = "0 0 * * * ?") // 매시간 실행
    public void refreshExpiringProductsCache() {
        log.info("만료 임박 상품 할인 캐시 갱신 시작");

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(3);
        // 내일까지 만료되는 상품들 조회
        List<Product> expiringProducts = productRepository
                .findByExpiryDateBeforeAndStatusNot(tomorrow, '0');

        List<Long> productIds = expiringProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        // 해당 상품들의 캐시 무효화 후 재계산
        discountService.invalidateCaches(productIds);

        // 미리 캐시 워밍업
        expiringProducts.forEach(product ->
                discountService.getCurrentDiscountRate(product.getId()));

        log.info("만료 임박 상품 할인 캐시 갱신 완료: {}개 상품", productIds.size());
    }
}
