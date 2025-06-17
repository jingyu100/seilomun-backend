package com.yju.team2.seilomun.domain.review.dto;

import com.yju.team2.seilomun.domain.review.entity.ReviewComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewCommentResponseDto {
    private Long id;
    private String content;
    private String sellerName;
    private LocalDateTime createdAt;

    public static ReviewCommentResponseDto fromEntity(ReviewComment reviewComment) {
        return ReviewCommentResponseDto.builder()
                .id(reviewComment.getId())
                .content(reviewComment.getContent())
                .sellerName(reviewComment.getSeller().getStoreName())
                .createdAt(reviewComment.getCreated_at())
                .build();
    }
}
