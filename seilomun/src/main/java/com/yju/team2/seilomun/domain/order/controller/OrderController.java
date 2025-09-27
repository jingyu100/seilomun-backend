package com.yju.team2.seilomun.domain.order.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.config.TossPaymentConfig;
import com.yju.team2.seilomun.domain.auth.dto.CartItemRequestDto;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.order.dto.*;
import com.yju.team2.seilomun.domain.order.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final TossPaymentConfig tossPaymentConfig;

    @PostMapping("/buy")
    public ResponseEntity<ApiResponseJson> buyProduct(@RequestBody OrderDto orderDto,
                                                      @AuthenticationPrincipal JwtUserDetails userDetail) {
        Long customerId = userDetail.getId();
        PaymentResDto paymentResDto = orderService.buyProduct(orderDto, customerId);
        paymentResDto.setSuccessUrl(orderDto.getYourSuccessUrl() == null ? tossPaymentConfig.getSuccessUrl() : orderDto.getYourSuccessUrl());
        paymentResDto.setFailUrl(orderDto.getYourFailUrl() == null ? tossPaymentConfig.getFailUrl() : orderDto.getYourFailUrl());
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Update", paymentResDto,
                        "Message", "상품이 주문 되었습니다")));
    }

    // 바로 구매하기
    @GetMapping("/buy")
    public ResponseEntity<ApiResponseJson> getBuyProduct(@RequestBody CartItemRequestDto cartItemRequestDto,
                                                         @AuthenticationPrincipal JwtUserDetails userDetail) {
        Long customerId = userDetail.getId();
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("주문페이지로 갑니다", orderService.getBuyProduct(cartItemRequestDto, customerId))));
    }

    @PostMapping("/buy/app")
    public ResponseEntity<ApiResponseJson> buyProductForApp(
            @RequestBody OrderDto orderDto,
            @AuthenticationPrincipal JwtUserDetails userDetail) {

        Long customerId = userDetail.getId();
        PaymentResDto paymentResDto = orderService.buyProduct(orderDto, customerId);

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of(
                        "amount", paymentResDto.getAmount(),
                        "orderId", paymentResDto.getTransactionId(),  // 토스에서 쓸 주문번호
                        "orderName", paymentResDto.getOrderName(),
                        "customerEmail", paymentResDto.getCustomerEmail(),
                        "customerName", paymentResDto.getCustomerName(),
                        "dbOrderId", paymentResDto.getOrderId(),  // DB의 주문 ID 
                        "message", "결제 정보가 생성되었습니다"
                )));
    }

    @PostMapping("/cart/buy")
    public ResponseEntity<ApiResponseJson> getBuyProducts(@RequestBody List<CartItemRequestDto> cartItemRequestDto,
                                                          @AuthenticationPrincipal JwtUserDetails userDetail) {
        Long customerId = userDetail.getId();
        List<OrderProductDto> orderProducts = orderService.getBuyProducts(cartItemRequestDto, customerId);

        // 판매자 ID도 함께 반환
        Long sellerId = null;
        if (!orderProducts.isEmpty()) {
            sellerId = orderService.getSellerIdFromProduct(orderProducts.get(0).getProductId());
        }

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of(
                        "orderProducts", orderProducts,
                        "sellerId", sellerId,
                        "message", "주문페이지로 갑니다"
                )));
    }

    // 결제 성공시 콜백
    // 여기서 orderId는 결제테이블의 pk가 아닌 결제고유식별자를 의미함
    @GetMapping("/toss/success")
    public ResponseEntity<ApiResponseJson> tossPaymentSuccess(@RequestParam String paymentKey,
                                                              @RequestParam String orderId,
                                                              @RequestParam Integer amount) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("성공", orderService.tossPaymentSuccess(paymentKey, orderId, amount),
                        "message", "결제가 성공적으로 완료되었습니다.")));
    }

    // 결제 실패시 콜백
    @GetMapping("/toss/fail")
    public ResponseEntity<ApiResponseJson> tossPaymentFail(@RequestParam String code,
                                                           @RequestParam String message,
                                                           @RequestParam String orderId) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("실패", orderService.tossPaymentFail(code, message, orderId),
                        "message", "결제가 실패 되었습니다.")));
    }

    // 판매자가 주문 수락
    @PostMapping("/acceptance/{orderId}")
    public ResponseEntity<ApiResponseJson> acceptOrder(
            @AuthenticationPrincipal JwtUserDetails seller,
            @PathVariable Long orderId) {
        orderService.acceptanceOrder(seller.getId(), orderId);
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("message", "주문이 수락 되었습니다.")
        ));
    }

    //판매자가 주문 거절
    @PostMapping("/refuse/{orderId}")
    public ResponseEntity<ApiResponseJson> refuseOrder(
            @AuthenticationPrincipal JwtUserDetails seller,
            @PathVariable Long orderId) {
        orderService.refuseOrder(seller.getId(), orderId);
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("message", "주문이 거절 되었습니다.")
        ));
    }

    //소비자가 하는 주문 취소
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<ApiResponseJson> cancelOrder(
            @AuthenticationPrincipal JwtUserDetails customer,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                orderService.cancelPayment(customer.getId(), orderId)));
    }

    // 환불 신청
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponseJson> refundOrder(
            @AuthenticationPrincipal JwtUserDetails customer,
            @PathVariable Long orderId,
            @RequestPart("refund") @Valid RefundRequestDto refundRequestDto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {

        try {
            RefundRequestDto requestDto = orderService.refundApplication(
                    customer.getId(), orderId, refundRequestDto, photos);

            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of(
                            "message", "환불 신청이 완료되었습니다.",
                            "refund", requestDto
                    )
            ));
        } catch (Exception e) {
            log.error("환불 신청 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseJson(HttpStatus.INTERNAL_SERVER_ERROR, Map.of(
                            "error", "환불 신청에 실패했습니다: " + e.getMessage()
                    )));
        }
    }

    // 환불 신청 수락
    @PostMapping("/refund/acceptance/{refundId}")
    public ResponseEntity<ApiResponseJson> refundAcceptOrder(
            @AuthenticationPrincipal JwtUserDetails seller,
            @PathVariable Long refundId) {
        orderService.refundAcceptance(seller.getId(), refundId);
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("message", "환불 신청 수락 완료")
        ));
    }

    // 환불 신청 거절
    @PostMapping("/refund/decline/{refundId}")
    public ResponseEntity<ApiResponseJson> refundDeclineOrder(
            @AuthenticationPrincipal JwtUserDetails seller,
            @PathVariable Long refundId) {
        orderService.refundDecline(seller.getId(), refundId);
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("message", "환불 신청 거절 완료")
        ));
    }
    
    // 통계 조회
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseJson> getStats(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                    @RequestParam(defaultValue = "monthly") String period,
                                                    @RequestParam(required = false) Integer year,
                                                    @RequestParam(required = false) Integer month) {

        Long SellerId = userDetails.getId();

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("통계 조회",orderService.getStats(SellerId,period,year,month))));
    }
    
    // SDK창 닫을때
    @PostMapping("/close-payment/{orderId}")
    public ResponseEntity<ApiResponseJson> closePayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtUserDetails userDetail) {
        log.info("결제창 닫기 요청: orderId={}, customerId={}", orderId, userDetail.getId());
        try {
            orderService.closePayment(userDetail.getId(), orderId);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("message", "주문이 성공적으로 취소되었습니다",
                            "orderId", orderId)));
        } catch (Exception e) {
            log.error("결제창 닫기 처리 실패: orderId={}, customerId={}, error={}",
                    orderId, userDetail.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseJson(HttpStatus.BAD_REQUEST,
                    Map.of("error", e.getMessage())));
        }
    }
}
