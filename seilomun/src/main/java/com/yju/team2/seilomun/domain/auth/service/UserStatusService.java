package com.yju.team2.seilomun.domain.auth.service;

import com.yju.team2.seilomun.domain.auth.dto.UserStatusDto;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatusService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;

    private static final String USER_ONLINE_KEY_PREFIX = "user:online:";
    // 5분
    private static final Duration ONLINE_TIMEOUT = Duration.ofMinutes(5);

    // 소비자만 redis로 온라인 상태 관리
    public void updateOnlineStatus(String email, String userType) {
        try {
            if ("CUSTOMER".equals(userType)) {
                Long customerId = getCustomerIdByEmail(email);
                if (customerId != null) {
                    String userKey = USER_ONLINE_KEY_PREFIX + "CUSTOMER:" + customerId;
                    redisTemplate.opsForValue().set(userKey, String.valueOf(System.currentTimeMillis()), ONLINE_TIMEOUT);
                    log.debug("소비자 온라인 상태 업데이트:{}", email);
                }
            }
        } catch (Exception e) {
            log.error("온라인 상태 업데이트 실패: email={}, userType={}", email, userType, e);
        }
    }

    public void removeOnlineStatus(String email) {
        try{
            Long customerId = getCustomerIdByEmail(email);
            if (customerId != null) {
                String userKey = USER_ONLINE_KEY_PREFIX + "CUSTOMER:" + customerId;
                redisTemplate.delete(userKey);
                log.debug("소비자 온라인 상태 제거: {}", email);
            }
        } catch (Exception e){
            log.error("온라인 상태 제거 실패: email={}", email, e);
        }
    }

    // 통합 상태 조회 메서드
    public UserStatusDto getUserStatus(Long userId, String userType) {
        if ("CUSTOMER".equals(userType)) {
            return getCustomerStatus(userId);
        } else if ("SELLER".equals(userType)) {
            return getSellerStatus(userId);
        }
        return new UserStatusDto(false, "UNKNOWN", null);
    }

    private UserStatusDto getCustomerStatus(Long customerId) {
        String userKey = USER_ONLINE_KEY_PREFIX + "CUSTOMER:" + customerId;
        String lastActivity = (String) redisTemplate.opsForValue().get(userKey);

        if (lastActivity == null) {
            return new UserStatusDto(false, "OFFLINE", null);
        }

        try {
            long lastActivityTime = Long.parseLong(lastActivity);
            boolean isOnline = (System.currentTimeMillis() - lastActivityTime) < 300000;
            return new UserStatusDto(isOnline, isOnline ? "ONLINE" : "OFFLINE", null);
        } catch (NumberFormatException e) {
            return new UserStatusDto(false, "OFFLINE", null);
        }
    }

    private UserStatusDto getSellerStatus(Long sellerId) {
        Optional<Seller> sellerOptional = sellerRepository.findById(sellerId);
        if (sellerOptional.isEmpty()) {
            return new UserStatusDto(false, "NOT_FOUND", null);
        }

        Seller seller = sellerOptional.get();
        Character isOpen = seller.getIsOpen();

        // Seller의 isOpen 값에 따라 상태 결정
        String status;
        boolean isAvailable;

        switch (isOpen) {
            case '1': // 영업중
                status = "OPEN";
                isAvailable = true;
                break;
            case '0': // 영업종료
                status = "CLOSED";
                isAvailable = false;
                break;
            case '2': // 브레이크타임 (가정)
                status = "BREAK";
                isAvailable = false;
                break;
            default:
                status = "UNKNOWN";
                isAvailable = false;
        }

        return new UserStatusDto(isAvailable, status, seller.getOperatingHours());
    }

    private Long getCustomerIdByEmail(String email) {
        return customerRepository.findByEmail(email)
                .map(Customer::getId)
                .orElse(null);
    }
}
