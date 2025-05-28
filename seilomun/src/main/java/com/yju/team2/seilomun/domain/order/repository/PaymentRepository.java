package com.yju.team2.seilomun.domain.order.repository;

import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String orderId);

    Optional<Payment> findByOrderAndPaySuccessYN(Order order, boolean paySuccess);

    Optional<Payment> findByIdAndPaySuccessYN(Long id, boolean paySuccess);
}
