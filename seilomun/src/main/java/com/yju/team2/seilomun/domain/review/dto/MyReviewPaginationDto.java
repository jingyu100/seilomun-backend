package com.yju.team2.seilomun.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyReviewPaginationDto {
    private List<MyReviewResponseDto> myReviews;
    private boolean hasNext;
    private long totalElements;
}