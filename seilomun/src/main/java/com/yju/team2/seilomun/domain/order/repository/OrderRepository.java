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

    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId ORDER BY o.createdAt DESC")
    Page<Order> findBySellerIdWithPagination(@Param("sellerId") Long sellerId, Pageable pageable);

    Optional<Order> findByIdAndOrderStatus(Long orId, Character orderStatus);

    //연도별 통계
    @Query("SELECT new com.yju.team2.seilomun.domain.order.dto.StatsDto(" +
            "YEAR(o.createdAt), " +
            "SUM(oi.quantity), " +
            "SUM(o.totalAmount)) " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE o.orderStatus = 'A' "+
            "AND o.seller.id = :id " +
            "GROUP BY YEAR(o.createdAt) " +
            "ORDER BY YEAR(o.createdAt)")
    List<StatsDto> getYearStats(@Param("id") Long sellerId);

    // 월별 통계
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
            "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
            "ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<StatsDto> getMonthStats(@Param("id") Long sellerId,
                         @Param("year") Integer year,
                         @Param("month") Integer month);
    
    //일별 통계
    @Query("SELECT new com.yju.team2.seilomun.domain.order.dto.StatsDto(" +
            "YEAR(o.createdAt), " +
            "MONTH(o.createdAt), " +
            "DAY(o.createdAt)," +
            "SUM(oi.quantity), " +
            "SUM(o.totalAmount)) " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE o.orderStatus = 'A' "+
            "AND o.seller.id = :id " +
            "AND YEAR(o.createdAt) = :year " +
            "AND MONTH(o.createdAt) = :month " +
            "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt) " +
            "ORDER BY YEAR (o.createdAt), MONTH(o.createdAt), DAY(o.createdAt)")
    List<StatsDto> getDailyStats(@Param("id") Long sellerId,
                         @Param("year") Integer year,
                         @Param("month") Integer month);

    // 주별 통계
    @Query(value = "SELECT " +
            "YEAR(created_at) as year, " +
            "WEEK(created_at, 1) as week, " +
            "SUM(oi.quantity) as count, " +
            "SUM(o.total_amount) as totalAmount " +
            "FROM orders o " +
            "JOIN order_items oi ON o.or_id = oi.or_id " +
            "WHERE o.order_status = 'A' " +
            "AND o.se_id = :id " +
            "AND (:year IS NULL OR YEAR(o.created_at) = :year) " +
            "GROUP BY YEAR(o.created_at), WEEK(o.created_at, 1) " +
            "ORDER BY YEAR(o.created_at), WEEK(o.created_at, 1)",
            nativeQuery = true)
    List<Object[]> getWeeklyStatsRaw(@Param("id") Long sellerId,
                                     @Param("year") Integer year);

    // 분기별 통계
    @Query(value = "SELECT " +
            "YEAR(created_at) as year, " +
            "QUARTER(created_at) as quarter, " +
            "SUM(oi.quantity) as count, " +
            "SUM(o.total_amount) as totalAmount " +
            "FROM orders o " +
            "JOIN order_items oi ON o.or_id = oi.or_id " +
            "WHERE o.order_status = 'A' " +
            "AND o.se_id = :id " +
            "AND (:year IS NULL OR YEAR(o.created_at) = :year) " +
            "GROUP BY YEAR(o.created_at), QUARTER(o.created_at) " +
            "ORDER BY YEAR(o.created_at), QUARTER(o.created_at)",
            nativeQuery = true)
    List<Object[]> getQuarterStats(@Param("id") Long sellerId,
                                        @Param("year") Integer year);

}
