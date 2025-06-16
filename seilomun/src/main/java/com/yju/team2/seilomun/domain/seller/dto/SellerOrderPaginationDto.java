package com.yju.team2.seilomun.domain.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerOrderPaginationDto {
    private List<SellerOrderListResponseDto> orders;
    private boolean hasNext;
    private long totalElements;

}
