package com.yju.team2.seilomun.domain.review.dto;

import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import com.yju.team2.seilomun.domain.review.entity.Review;
import com.yju.team2.seilomun.domain.review.entity.ReviewPhoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyReviewResponseDto {
    private Long reviewId;
    private String reviewContent;
    private String storeName;
    private List<String> orderItems;
    private List<String> reviewPhotoUrls;
    private Integer rating;
    private LocalDateTime createdAt;
    private ReviewCommentResponseDto comment;
    private Long orderId;

    public static MyReviewResponseDto fromEntity(Review review) {
        // 주문한 상품 이름 목록
        List<String> itemNames = new ArrayList<>();
        List<OrderItem> orderItemList = review.getOrder().getOrderItems();
        for (OrderItem item : orderItemList) {
            itemNames.add(item.getProduct().getName());
        }

        // 리뷰 사진 URL 목록
        List<String> photoUrls = new ArrayList<>();
        List<ReviewPhoto> photos = review.getReviewPhotos();
        for (ReviewPhoto photo : photos) {
            photoUrls.add(photo.getPhoto_url());
        }

        return MyReviewResponseDto.builder()
                .reviewId(review.getId())
                .reviewContent(review.getContent())
                .storeName(review.getOrder().getSeller().getStoreName())
                .orderItems(itemNames)
                .reviewPhotoUrls(photoUrls)
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .orderId(review.getOrder().getId())
                .build();
    }
}
