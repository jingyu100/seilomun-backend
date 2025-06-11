package com.yju.team2.seilomun.domain.order.repository;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.order.dto.StatsDto;
import com.yju.team2.seilomun.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Boolean existsByOrderNumber(String orderName);
    List<Order> findByCustomer(Customer customer);

    Optional<Order> findByOrderNumber(String orderId);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    Page<Order> findByCustomerIdWithPagination(@Param("customerId") Long customerId, Pageable pageable);

    Optional<Order> findByIdAndOrderStatus(Long orId, Character orderStatus);

    @Query("SELECT new com.yju.team2.seilomun.domain.order.dto.StatsDto(" +
            "YEAR(o.createdAt), " +
            "MONTH(o.createdAt), " +
            "SUM(oi.quantity), " +
            "SUM(o.totalAmount)) " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE o.orderStatus = 'A' "+
            "AND o.seller.id = :id " +
            "AND YEAR(o.createdAt) = :year " +
            "AND MONTH(o.createdAt) = :month " +
            "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<StatsDto> stats(@Param("id") Long sellerId,
                         @Param("year") Integer year,
                         @Param("month") Integer month);

}
