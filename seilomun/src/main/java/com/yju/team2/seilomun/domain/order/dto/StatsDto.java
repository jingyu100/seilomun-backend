package com.yju.team2.seilomun.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDto {

    private int year;
    private int month;
    private Long count;
    private Long totalAmount;

}
