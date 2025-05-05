package com.yju.team2.seilomun.domain.review.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequestDto {
    private String reviewContent;
    private Integer rating;
    @Size(max = 5, message = "사진은 최대 5장까지 업로드할 수 있습니다.")
    private List<String> reviewPhotos;
}
