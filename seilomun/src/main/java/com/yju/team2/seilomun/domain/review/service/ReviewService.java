package com.yju.team2.seilomun.domain.review.service;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.notification.event.ReviewWrittenEvent;
import com.yju.team2.seilomun.domain.notification.service.NotificationService;
import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.order.repository.OrderRepository;
import com.yju.team2.seilomun.domain.review.dto.ReviewCommentRequestDto;
import com.yju.team2.seilomun.domain.review.dto.ReviewPaginationDto;
import com.yju.team2.seilomun.domain.review.dto.ReviewRequestDto;
import com.yju.team2.seilomun.domain.review.dto.ReviewResponseDto;
import com.yju.team2.seilomun.domain.review.entity.Review;
import com.yju.team2.seilomun.domain.review.entity.ReviewComment;
import com.yju.team2.seilomun.domain.review.entity.ReviewPhoto;
import com.yju.team2.seilomun.domain.review.repository.ReviewCommentRepository;
import com.yju.team2.seilomun.domain.review.repository.ReviewPhotoRepository;
import com.yju.team2.seilomun.domain.review.repository.ReviewRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.domain.upload.service.AWSS3UploadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final OrderRepository orderRepository;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final ReviewCommentRepository commentRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final AWSS3UploadService awsS3UploadService;
    private final NotificationService notificationService;

    @Transactional
    public ReviewRequestDto postReview(Long customerId,Long orderId ,ReviewRequestDto reviewRequestDto, List<MultipartFile> photos) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자 입니다.");
        }
        Customer customer = optionalCustomer.get();
        Optional<Order> optionalOrder = orderRepository.findByIdAndOrderStatus(orderId,'A');
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 주문 입니다.");
        }
        Order order = optionalOrder.get();
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("해당 주문에 접근 권한이 없습니다.");
        }
        Optional<Review> optionalReview = reviewRepository.findByOrder(order);
        if (optionalReview.isPresent()) {
            throw new IllegalArgumentException("주문당 한개의 리뷰만 작성할 수 있습니다.");
        }

        // 사진 업로드 처리
        List<String> photoUrls = new ArrayList<>();
        if (photos != null && !photos.isEmpty()) {
            if (photos.size() > 5) {
                throw new IllegalArgumentException("사진은 최대 5장까지 업로드할 수 있습니다.");
            }

            try {
                photoUrls = awsS3UploadService.uploadFiles(photos);
                log.info("리뷰 사진 업로드 완료: {} 장", photoUrls.size());
            } catch (Exception e) {
                log.error("리뷰 사진 업로드 실패: {}", e.getMessage());
                throw new RuntimeException("사진 업로드에 실패했습니다.");
            }
        }
        // 리뷰 생성
        Review review = Review.builder().
                content(reviewRequestDto.getReviewContent()).
                rating(reviewRequestDto.getRating()).
                order(order).
                build();
        reviewRepository.save(review);
        // 리뷰 사진 등록 (기존 URL 리스트 + 새로 업로드된 사진들)
        List<String> allPhotoUrls = new ArrayList<>();
        if (reviewRequestDto.getReviewPhotos() != null) {
            allPhotoUrls.addAll(reviewRequestDto.getReviewPhotos());
        }
        allPhotoUrls.addAll(photoUrls);

        if (!allPhotoUrls.isEmpty()) {
            allPhotoUrls.forEach(url -> {
                reviewPhotoRepository.save(ReviewPhoto.builder()
                        .review(review)
                        .photo_url(url)
                        .build());
            });
        }
        // 별점 계산
        Seller seller = order.getSeller();
        List<Review> sellerReviews = reviewRepository.findAllByOrder_SellerId(seller.getId());

        float totalRating = 0f;
        for (Review sellerReview : sellerReviews) {
            totalRating += sellerReview.getRating();
        }
        float averageRating;
        if (sellerReviews.isEmpty()) {
            averageRating = 0f;
        } else {
            averageRating = totalRating / sellerReviews.size();
            // 소수점 첫째 자리까지 반올림
            averageRating = Math.round(averageRating * 10) / 10.0f;
        }
        seller.updateRating(averageRating);
        reviewRequestDto.setReviewPhotos(allPhotoUrls);

        try {
            ReviewWrittenEvent reviewWrittenEvent = ReviewWrittenEvent.builder()
                    .review(review)
                    .eventId("REVIEW_" + review.getId())
                    .build();
            notificationService.processNotification(reviewWrittenEvent);
        } catch (Exception e) {
            log.error("리뷰 작성 알림 전송 실패", e);
        }

        return reviewRequestDto;
    }

    // 리뷰 조회하기
    public ReviewPaginationDto getReviews(Long sellerId, int page, int size) {
        Optional<Seller> optionalSeller = sellerRepository.findById(sellerId);
        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 판매자 입니다.");
        }
        Seller seller = optionalSeller.get();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findAllBySellerIdWithPagination(sellerId, pageable);

        List<ReviewResponseDto> reviewResponseDtos = new ArrayList<>();
        for (Review review : reviewPage.getContent()) {
            reviewResponseDtos.add(ReviewResponseDto.fromEntity(review));
        }

        return ReviewPaginationDto.builder()
                .reviews(reviewResponseDtos)
                .hasNext(reviewPage.hasNext())
                .totalElements(reviewPage.getTotalElements())
                .build();
    }

    @Transactional
    public ReviewCommentRequestDto postComment(Long sellerID, Long reviewId, ReviewCommentRequestDto reviewCommentRequestDto) {
        Optional<Seller> optionalSeller = sellerRepository.findById(sellerID);
        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 판매자 입니다.");
        }
        Seller seller = optionalSeller.get();
        Optional<Review> optionalReview = reviewRepository.findById(reviewId);
        if (optionalReview.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 리뷰 입니다.");
        }
        Review review = optionalReview.get();
        if (!review.getOrder().getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("해당 리뷰에 접근 권한이 없습니다.");
        }
        ReviewComment reviewComment = ReviewComment.builder().
                content(reviewCommentRequestDto.getReviewComment()).
                review(review).
                seller(seller).
                build();
        reviewCommentRepository.save(reviewComment);
        return reviewCommentRequestDto;
    }
}
