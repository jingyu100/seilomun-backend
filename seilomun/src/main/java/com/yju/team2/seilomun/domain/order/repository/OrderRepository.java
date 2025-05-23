package com.yju.team2.seilomun.domain.order.repository;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Boolean existsByOrderNumber(String orderName);
    List<Order> findByCustomer(Customer customer);

    Optional<Order> findByOrderNumber(String orderId);
}
