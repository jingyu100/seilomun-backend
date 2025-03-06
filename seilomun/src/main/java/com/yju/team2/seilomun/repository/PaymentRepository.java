package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
