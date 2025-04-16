package com.yju.team2.seilomun.domain.order.repository;

import com.yju.team2.seilomun.domain.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String orderId);
}
