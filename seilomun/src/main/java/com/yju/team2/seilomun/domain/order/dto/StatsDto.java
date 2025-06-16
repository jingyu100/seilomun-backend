package com.yju.team2.seilomun.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StatsDto {

    private Integer year;
    private Integer month;
    private Integer day;
    private Integer week;
    private Integer quarter;
    private Integer count;
    private Integer totalAmount;

    // 년도별 통계
    public StatsDto(Integer year, Long count, Long totalAmount) {
        this.year = year;
        this.count = count.intValue();
        this.totalAmount = totalAmount.intValue();
    }

    // 월별 통계
    public StatsDto(Integer year, Integer month, Long count, Long totalAmount) {
        this.year = year;
        this.month = month;
        this.count = count.intValue();
        this.totalAmount = totalAmount.intValue();
    }

    // 일별 통계
    public StatsDto(Integer year, Integer month, Integer day, Long count, Long totalAmount) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.count = count.intValue();
        this.totalAmount = totalAmount.intValue();
    }

}
