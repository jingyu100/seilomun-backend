package com.yju.team2.seilomun.domain.review.repository;

import com.yju.team2.seilomun.domain.review.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
}
