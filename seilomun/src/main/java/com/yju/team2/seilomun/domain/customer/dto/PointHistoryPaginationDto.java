package com.yju.team2.seilomun.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PointHistoryPaginationDto {
    private List<PointHistoryResDto> pointHistories;
    private Integer currentPoints; // 현재 포인트
    private boolean hasNext;
    private long totalElements;
}
