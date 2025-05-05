package com.yju.team2.seilomun.domain.review.repository;

import com.yju.team2.seilomun.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByOrder_SellerId(Long sellerId);
}
