package com.yju.team2.seilomun.domain.product.repository;

import com.yju.team2.seilomun.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    //현재시간 이전이거나 상태가 0 이 아닌거 찾는거
    List<Product> findByExpiryDateBeforeAndStatusNot(LocalDateTime dateTime, Character status);
}
