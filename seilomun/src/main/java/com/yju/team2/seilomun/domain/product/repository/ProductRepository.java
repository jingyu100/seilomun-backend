package com.yju.team2.seilomun.domain.product.repository;

import com.yju.team2.seilomun.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    //현재시간 이전이거나 상태가 0 이 아닌거 찾는거
    List<Product> findByExpiryDateBeforeAndStatusNot(LocalDateTime dateTime, Character status);
    // 특정 상태의 상품 중 유통기한이 특정 날짜 이전인 상품 찾기
    List<Product> findByExpiryDateBeforeAndStatus(LocalDateTime dateTime, Character status);
    List<Product> findBySellerId(Long SellerId);
}
