package com.yju.team2.seilomun.domain.order.controller;

import com.yju.team2.seilomun.config.TossPaymentConfig;
import com.yju.team2.seilomun.domain.auth.CartItemRequestDto;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.order.service.OrderService;
import com.yju.team2.seilomun.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/buy")
    public ResponseEntity<ApiResponseJson> getBuyProduct(@RequestBody CartItemRequestDto cartItemRequestDto,
                                                         @AuthenticationPrincipal JwtUserDetails userDetail) {
        Long customerId = userDetail.getId();
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("주문페이지로 갑니다", orderService.getBuyProduct(cartItemRequestDto, customerId))));
    }
    // 결제 성공시 콜백
    // 여기서 orderId는 결제테이블의 pk가 아닌 결제고유식별자를 의미함
    @GetMapping("/toss/success")
    public ResponseEntity<ApiResponseJson> tossPaymentSuccess(@RequestParam String paymentKey,
                                                              @RequestParam String orderId,
                                                              @RequestParam Integer amount) {
        PaymentSuccessDto paymentSuccessDto = orderService.tossPaymentSuccess(paymentKey, orderId, amount);

        // ApiResponseJson 형태로 감싸서 반환
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("data", paymentSuccessDto,
                        "message", "결제가 성공적으로 처리되었습니다.")));
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
    //테스트 결제 할려고 만든거
    @PostMapping("/test/buy")
    public ResponseEntity<ApiResponseJson> testBuyProduct(@RequestBody OrderDto orderDto) {
        Long testCustomerId = 1L;

        PaymentResDto paymentResDto = orderService.buyProduct(orderDto, testCustomerId);
        paymentResDto.setSuccessUrl(orderDto.getYourSuccessUrl() == null ? tossPaymentConfig.getSuccessUrl() : orderDto.getYourSuccessUrl());
        paymentResDto.setFailUrl(orderDto.getYourFailUrl() == null ? tossPaymentConfig.getFailUrl() : orderDto.getYourFailUrl());

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Update", paymentResDto,
                        "Message", "상품이 주문 되었습니다")));
    }
}
