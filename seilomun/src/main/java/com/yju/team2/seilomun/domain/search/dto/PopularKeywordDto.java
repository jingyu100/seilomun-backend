package com.yju.team2.seilomun.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PopularKeywordDto {

    private String keyword;

    private Long count;

}
