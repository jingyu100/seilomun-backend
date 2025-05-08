package com.yju.team2.seilomun.domain.review.repository;

import com.yju.team2.seilomun.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByOrder_SellerId(Long sellerId);

    @Query("SELECT r FROM Review r JOIN r.order o WHERE o.seller.id = :sellerId ORDER BY r.createdAt DESC")
    Page<Review> findAllBySellerIdWithPagination(@Param("sellerId") Long sellerId, Pageable pageable);
}
