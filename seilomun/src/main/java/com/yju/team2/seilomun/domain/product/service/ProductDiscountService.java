package com.yju.team2.seilomun.domain.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yju.team2.seilomun.domain.product.dto.DiscountInfo;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDiscountService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DISCOUNT_RATE_KEY = "discount:rate:";
    private static final String DISCOUNT_INFO_KEY = "discount:info:";
    private static final long CACHE_EXPIRATION_SECONDS = 30 * 60; // 30분

    // 상품의 현재 할인율 조회 ( 캐시 우선 확인 )
    public Integer getCurrentDiscountRate(Long productId) {

        // 캐시에서 먼저 조회
        String cacheKey = DISCOUNT_RATE_KEY + productId;
        String cachedRate = redisTemplate.opsForValue().get(cacheKey);

        if (cachedRate != null) {
            log.debug("할인율 캐시 히트: productId={}, rate={}", productId, cachedRate);
            return Integer.parseInt(cachedRate);
        }

        // 캐시에서 미스 시 계산 후 캐시 저장
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다: " + productId));

        Integer discountRate = calculateDiscountRate(product);

        // 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(discountRate),
                CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);

        log.debug("할인율 계산 후 캐시 저장: productId={}, rate={}", productId, discountRate);
        return discountRate;
    }

    // 할인된 가격 조회 ( 캐시 우선 )
    public Integer getDiscountedPrice(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다: " + productId));

        Integer discountRate = getCurrentDiscountRate(productId);
        return calculateDiscountedPrice(product.getOriginalPrice(), discountRate);
    }

    // 할인 정보 일괄 조회 ( 할인율 + 할인가격 )
    public DiscountInfo getDiscountInfo(Long productId) {
        String cacheKey = DISCOUNT_INFO_KEY + productId;
        String cachedInfo = redisTemplate.opsForValue().get(cacheKey);

        if (cachedInfo != null) {
            try {
                return objectMapper.readValue(cachedInfo, DiscountInfo.class);
            } catch (Exception e) {
                log.warn("할인 정보 캐시 파싱 실패: productId={}", productId, e);
                // 캐시 파싱 실패 시 캐시 삭제 후 재계산
                redisTemplate.delete(cacheKey);
            }
        }

        // 캐시 미스 시 계산
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다: " + productId));

        Integer discountRate = calculateDiscountRate(product);
        Integer discountedPrice = calculateDiscountedPrice(product.getOriginalPrice(), discountRate);

        DiscountInfo discountInfo = DiscountInfo.builder()
                .productId(productId)
                .originalPrice(product.getOriginalPrice())
                .discountRate(discountRate)
                .discountedPrice(discountedPrice)
                .calculatedAt(LocalDateTime.now())
                .build();

        // 캐시에 저장
        try {
            String serialized = objectMapper.writeValueAsString(discountInfo);
            redisTemplate.opsForValue().set(cacheKey, serialized,
                    CACHE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("할인 정보 캐시 저장 실패: productId={}", productId, e);
        }

        return discountInfo;
    }

    // 할인율 계산 로직
    private Integer calculateDiscountRate(Product product) {
        // 기본 검증
        if (product.getExpiryDate() == null ||
                product.getMinDiscountRate() == null ||
                product.getMaxDiscountRate() == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = product.getCreatedAt();
        LocalDateTime expiryDate = product.getExpiryDate();

        // 유통기한이 지난 경우
        if (now.isAfter(expiryDate)) {
            return product.getMaxDiscountRate(); // 최대 할인율 적용
        }

        // 전체 기간 계산
        long totalDays = ChronoUnit.DAYS.between(createdAt, expiryDate);
        if (totalDays <= 0) {
            return product.getMaxDiscountRate();
        }

        // 남은 기간 계산
        long remainingDays = ChronoUnit.DAYS.between(now, expiryDate);
        if (remainingDays < 0) {
            remainingDays = 0;
        }

        // 짧은 기간 상품은 최대 할인율 적용
        if (totalDays <= 3) {
            return product.getMaxDiscountRate();
        }

        // 선형 보간으로 할인율 계산
        double progressRatio = 1.0 - ((double) remainingDays / (double) totalDays);
        int interpolatedRate = (int) Math.round(
                product.getMinDiscountRate() +
                        (product.getMaxDiscountRate() - product.getMinDiscountRate()) * progressRatio
        );

        // 범위 제한
        int finalRate = Math.max(product.getMinDiscountRate(),
                Math.min(product.getMaxDiscountRate(), interpolatedRate));

        log.debug("할인율 계산: productId={}, 전체기간={}일, 남은기간={}일, 진행률={}, 할인율={}%",
                product.getId(), totalDays, remainingDays,
                String.format("%.2f", progressRatio), finalRate);

        return finalRate;
    }

    // 할인된 가격 계산
    private Integer calculateDiscountedPrice(Integer originalPrice, Integer discountRate) {
        if (originalPrice == null || discountRate == null || discountRate <= 0) {
            return originalPrice;
        }
        return originalPrice * (100 - discountRate) / 100;
    }


    // 캐시 무효화 (상품 정보 변경 시 호출)
    public void invalidateCache(Long productId) {
        String discountRateKey = DISCOUNT_RATE_KEY + productId;
        String discountInfoKey = DISCOUNT_INFO_KEY + productId;

        redisTemplate.delete(discountRateKey);
        redisTemplate.delete(discountInfoKey);

        log.info("할인 캐시 무효화: productId={}", productId);
    }

    // 여러 상품의 캐시 무효화
    public void invalidateCaches(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        List<String> keysToDelete = new ArrayList<>();
        for (Long productId : productIds) {
            keysToDelete.add(DISCOUNT_RATE_KEY + productId);
            keysToDelete.add(DISCOUNT_INFO_KEY + productId);
        }

        redisTemplate.delete(keysToDelete);
        log.info("할인 캐시 일괄 무효화: 상품 {}개", productIds.size());
    }

}
