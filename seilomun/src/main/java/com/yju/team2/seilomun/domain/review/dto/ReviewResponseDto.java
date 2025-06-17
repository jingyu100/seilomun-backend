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
public class ReviewResponseDto {
    private Long reviewId;
    private String reviewContent;
    private String customerPhoto;
    private String customerName;
    private List<String> orderItems;
    private List<String> reviewPhotoUrls;
    private String customerPhotos;
    private String sellerName;
    private Integer rating;
    private LocalDateTime createdAt;
    private ReviewCommentResponseDto comment;


    public static ReviewResponseDto fromEntity(Review review) {
        // 주문한 상품 이름 목록
        List<String> itemNames = new ArrayList<>();
        // 주문 상품 정보 추출
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

        // 리뷰 사진 URL을 쉼표로 구분된 문자열로 변환
        StringBuilder photoUrlBuilder = new StringBuilder();
        for (int i = 0; i < photos.size(); i++) {
            if (i > 0) {
                photoUrlBuilder.append(",");
            }
            photoUrlBuilder.append(photos.get(i).getPhoto_url());
        }

        return ReviewResponseDto.builder()
                .reviewId(review.getId())
                .reviewContent(review.getContent())
                .customerPhoto(review.getOrder().getCustomer().getProfileImageUrl())
                .customerName(review.getOrder().getCustomer().getName())
                .orderItems(itemNames)
                .reviewPhotoUrls(photoUrls)
                .customerPhotos(photoUrlBuilder.toString())
                .sellerName(review.getOrder().getSeller().getStoreName())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
