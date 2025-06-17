package com.yju.team2.seilomun.domain.review.repository;

import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByOrder_SellerId(Long sellerId);
    Optional<Review> findByOrder(Order order);

    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.seller " +
            "WHERE o.seller.id = :sellerId " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findAllBySellerIdWithPagination(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT DISTINCT r FROM Review r " +
            "LEFT JOIN FETCH r.reviewPhotos " +
            "WHERE r.id IN :reviewIds")
    List<Review> findReviewsWithPhotos(@Param("reviewIds") List<Long> reviewIds);

    @Query("SELECT DISTINCT r FROM Review r " +
            "LEFT JOIN FETCH r.order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE r.id IN :reviewIds")
    List<Review> findReviewsWithOrderItems(@Param("reviewIds") List<Long> reviewIds);
}
