package com.yju.team2.seilomun.domain.review.controller;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.review.dto.ReviewCommentRequestDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
            @RequestPart("review") @Valid ReviewRequestDto reviewRequestDto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        try {
            // 리뷰 작성 및 사진 업로드 처리
            ReviewRequestDto result = reviewService.postReview(userDetails.getId(), orderId, reviewRequestDto, photos);

            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                    "message", "리뷰 작성이 완료되었습니다.",
                    "review", result
            )));
        } catch (Exception e) {
            log.error("리뷰 작성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseJson(HttpStatus.INTERNAL_SERVER_ERROR, Map.of(
                            "error", "리뷰 작성에 실패했습니다: " + e.getMessage()
                    )));
        }
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


    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponseJson> deleteReview(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(userDetails.getId(), reviewId);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                    "message", "리뷰가 성공적으로 삭제되었습니다."
            )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseJson(HttpStatus.BAD_REQUEST, Map.of(
                            "error", e.getMessage()
                    )));
        } catch (Exception e) {
            log.error("리뷰 삭제 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseJson(HttpStatus.INTERNAL_SERVER_ERROR, Map.of(
                            "error", "리뷰 삭제에 실패했습니다: " + e.getMessage()
                    )));
        }
    }


    //사장님 리뷰 답글
    @PostMapping("/comment/{reviewId}")
    public ResponseEntity<ApiResponseJson> addComment(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody ReviewCommentRequestDto reviewCommentRequestDto){
        if (userDetails.isSeller()){
            return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                    "댓글 작성" , reviewService.postComment(userDetails.getId(), reviewId, reviewCommentRequestDto)
            ))));
        } else {
            return ResponseEntity.ok((new ApiResponseJson(HttpStatus.FORBIDDEN, Map.of())));
        }
    }
}
