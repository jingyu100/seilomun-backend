package com.yju.team2.seilomun.domain.order.repository;

import com.yju.team2.seilomun.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Boolean existsByOrderName(String orderName);
}
