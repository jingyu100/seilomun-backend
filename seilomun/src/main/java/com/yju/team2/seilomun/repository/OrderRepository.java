package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
