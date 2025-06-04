package com.yju.team2.seilomun.domain.report.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.report.dto.ReportDto;
import com.yju.team2.seilomun.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
@Slf4j
public class ReportController {
    private final ReportService reportService;

    // 소비자가 판매자 신고하는거
    @PostMapping("/customer/{orderId}")
    public ResponseEntity<ApiResponseJson> customerReport(
            @PathVariable Long orderId,
            @RequestBody ReportDto reportDto,
            @AuthenticationPrincipal JwtUserDetails customer) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("message", "주문 신고 완료",
                        "신고 내용", reportService.customerReport(orderId, customer.getId(),reportDto))
        ));
    }
    // 판매자가 소비자 신고하는거
    @PostMapping("/seller/{reviewId}")
    public ResponseEntity<ApiResponseJson> sellerReport(
            @PathVariable Long reviewId,
            @RequestBody ReportDto reportDto,
            @AuthenticationPrincipal JwtUserDetails seller) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("message", "리뷰 신고 완료",
                        "신고 내용", reportService.sellerReport(reviewId, seller.getId(),reportDto))
        ));
    }
}
