package com.yju.team2.seilomun.domain.order.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundRequestDto {
    private String refundType;
    private String title;
    private String content;
    @Size(max = 5, message = "사진은 최대 5장까지 업로드할 수 있습니다.")
    private List<String> refundPhotos;
}
