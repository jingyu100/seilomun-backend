package com.yju.team2.seilomun.domain.order.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.config.TossPaymentConfig;
import com.yju.team2.seilomun.domain.auth.dto.CartItemRequestDto;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.order.dto.OrderDto;
import com.yju.team2.seilomun.domain.order.dto.PaymentResDto;
import com.yju.team2.seilomun.domain.order.dto.PaymentSuccessDto;
import com.yju.team2.seilomun.domain.order.dto.RefundRequestDto;
import com.yju.team2.seilomun.domain.order.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // 장바구니에서 구매하기
    @GetMapping("/buy")
    public ResponseEntity<ApiResponseJson> getBuyProducts(@RequestBody List<CartItemRequestDto> cartItemRequestDto,
                                                          @AuthenticationPrincipal JwtUserDetails userDetail) {
        Long customerId = userDetail.getId();
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("주문페이지로 갑니다", orderService.getBuyProducts(cartItemRequestDto, customerId))));
    }

    // 결제 성공시 콜백
    // 여기서 orderId는 결제테이블의 pk가 아닌 결제고유식별자를 의미함
    @GetMapping("/toss/success")
    public void tossPaymentSuccess(@RequestParam String paymentKey,
                                   @RequestParam String orderId,
                                   @RequestParam Integer amount,
                                   HttpServletResponse response) throws IOException {
        PaymentSuccessDto paymentSuccessDto = orderService.tossPaymentSuccess(paymentKey, orderId, amount);

        // ApiResponseJson 형태로 감싸서 반환 , 나중에 프론트에서 리다이렉트하게 변경할수도 있음
        response.sendRedirect("http://localhost:5173/");
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
            @RequestBody @Valid RefundRequestDto refundRequestDto) {
        RefundRequestDto requestDto = orderService.refundApplication(customer.getId(), orderId, refundRequestDto);
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("환불 신청 완료", requestDto.getTitle())
        ));
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
    
    // 통계 조회
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseJson> getStats(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                    @RequestParam(required = false) Integer year,
                                                    @RequestParam(required = false) Integer month) {

        Long SellerId = userDetails.getId();

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("통계 조회",orderService.getStats(SellerId,year,month))));
    }
}
