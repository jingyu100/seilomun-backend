package com.yju.team2.seilomun.domain.review.repository;

import com.yju.team2.seilomun.domain.review.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    Optional<ReviewComment> findByReviewId(Long reviewId);
}
