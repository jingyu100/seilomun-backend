package com.yju.team2.seilomun.domain.report.service;

import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.order.repository.OrderRepository;
import com.yju.team2.seilomun.domain.report.dto.ReportDto;
import com.yju.team2.seilomun.domain.report.entity.Report;
import com.yju.team2.seilomun.domain.report.repository.ReportRepository;
import com.yju.team2.seilomun.domain.review.entity.Review;
import com.yju.team2.seilomun.domain.review.repository.ReviewRepository;
import com.yju.team2.seilomun.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReportService {
    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository;
    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    public ReportDto customerReport(Long orderId, Long customerId , ReportDto reportDto) {
        Optional<Order> optionalOrder = orderRepository.findByIdAndOrderStatus(orderId,'A');
        if (optionalOrder.isEmpty()){
            throw new IllegalArgumentException("주문이 존재하지 않습니다.");
        }
        Order order = optionalOrder.get();
        if (!order.getCustomer().getId().equals(customerId)){
            throw new IllegalArgumentException("해당 주문에 신고할 권한이 없습니다.");
        }
        Optional<Report> optionalReport = reportRepository.findByOrderAndTargetType(order,'S');
        if(optionalReport.isPresent()){
            throw new IllegalArgumentException("같은 주문에 또 신고를 할 수 없습니다.");
        }
        Report report = Report.builder().
                type(reportDto.getReportType()).
                content(reportDto.getContent()).
                target_type('S'). // 매장의 대한 신고니깐 'S'eller
                order(order).
                build();
        reportRepository.save(report);
        return ReportDto.builder().
                reportType(report.getType()).
                content(report.getContent()).
                build();
    }


    public ReportDto sellerReport(Long reviewId, Long sellerId , ReportDto reportDto) {
        Optional<Review> optionalReview = reviewRepository.findById(reviewId);
        if (optionalReview.isEmpty()){
            throw new IllegalArgumentException("리뷰가 존재 하지 않습니다");
        }
        Review review = optionalReview.get();
        Optional<Order> optionalOrder = orderRepository.findById(review.getOrder().getId());
        if (optionalOrder.isEmpty()){
            throw new IllegalArgumentException("주문이 존재하지 않습니다.");
        }
        Order order = optionalOrder.get();
        if (!order.getSeller().getId().equals(sellerId)){
            throw new IllegalArgumentException("해당 주문에 신고할 권한이 없습니다.");
        }
        Optional<Report> optionalReport = reportRepository.findByOrderAndTargetType(order,'C');
        if(optionalReport.isPresent()){
            throw new IllegalArgumentException("같은 리뷰에 또 신고를 할 수 없습니다.");
        }
        Report report = Report.builder().
                type(reportDto.getReportType()).
                content(reportDto.getContent()).
                target_type('C'). // 소비자의 리뷰 신고니깐 'C'ustomer
                order(order).
                build();
        reportRepository.save(report);
        return ReportDto.builder().
                reportType(report.getType()).
                content(report.getContent()).
                build();
    }
}
