package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.domain.review.dto.ReviewResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPaginationDto {
    private List<OrderListResponseDto> orders;
    private boolean hasNext;
    // 전체 갯수
    private long totalElements;
}