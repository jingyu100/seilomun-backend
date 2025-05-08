package com.yju.team2.seilomun.domain.review.controller;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.review.dto.ReviewPaginationDto;
import com.yju.team2.seilomun.domain.review.dto.ReviewRequestDto;
import com.yju.team2.seilomun.domain.review.service.ReviewService;
import com.yju.team2.seilomun.common.ApiResponseJson;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService reviewService;
    //리뷰 쓰기
    @PostMapping("/{orderId}")
    public ResponseEntity<ApiResponseJson> writeReview(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody @Valid ReviewRequestDto reviewRequestDto) {
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "리뷰 작성 완료", reviewService.postReview(userDetails.getId(), orderId, reviewRequestDto)
        ))));
    }
    
    //리뷰 불러오기
    @GetMapping("/{sellerId}")
    public ResponseEntity<ApiResponseJson> getReview(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ReviewPaginationDto reviews = reviewService.getReviews(sellerId, page, size);
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "리뷰 조회" , reviews
        ))));
    }
}
