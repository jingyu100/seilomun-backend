package com.yju.team2.seilomun.domain.order.repository;

import com.yju.team2.seilomun.domain.order.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByIdAndStatus(Long id, Character status);
}
