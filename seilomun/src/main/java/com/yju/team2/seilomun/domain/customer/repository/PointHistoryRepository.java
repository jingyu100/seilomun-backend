package com.yju.team2.seilomun.domain.customer.repository;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.entity.PointHistory;
import com.yju.team2.seilomun.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Integer> {
    Optional<PointHistory> findByOrderAndType(Order order,Character type);

    Page<PointHistory> findByCustomer(Customer customer, Pageable pageable);
}
