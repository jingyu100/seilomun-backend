package com.yju.team2.seilomun.domain.report.repository;

import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("SELECT r FROM Report r WHERE r.order = :order AND r.target_type = :targetType")
    Optional<Report> findByOrderAndTargetType(@Param("order") Order order,
                                              @Param("targetType") Character targetType);
}
