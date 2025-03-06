package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
