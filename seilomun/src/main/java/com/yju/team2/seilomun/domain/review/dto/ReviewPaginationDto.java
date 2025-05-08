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
public class ReviewPaginationDto {
    private List<ReviewResponseDto> reviews;
    private boolean hasNext;
    // 전체 갯수
    private long totalElements;
}
