package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.domain.customer.entity.PointHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PointHistoryResDto {
    private Long orderId;
    private Long pointId;
    private LocalDateTime createTime;
    private List<String> productName;
    private Integer pointAmount;
    private Character pointType;
    private String description;

    public static PointHistoryResDto fromEntity(PointHistory pointHistory) {
        String description = generateDescription(pointHistory);
        Long orderId = pointHistory.getOrder() != null ? pointHistory.getOrder().getId() : null;

        return PointHistoryResDto.builder()
                .pointId(pointHistory.getId())
                .pointType(pointHistory.getType())
                .pointAmount(pointHistory.getAmount())
                .description(description)
                .orderId(orderId)
                .createTime(pointHistory.getCreatedAt())
                .build();
    }

    private static String generateDescription(PointHistory pointHistory) {
        switch (pointHistory.getType()) {
            case 'A': // 적립
                return "주문 적립";
            case 'C': // 취소(차감)
                return "주문 취소 환수";
            case 'U': // 사용
                return "포인트 사용";
            default:
                return "포인트 내역";
        }
    }

}


