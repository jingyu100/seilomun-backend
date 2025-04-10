package com.yju.team2.seilomun.api.order;

import com.yju.team2.seilomun.config.TossPaymentConfig;
import com.yju.team2.seilomun.domain.auth.CartItemRequestDto;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.order.service.OrderService;
import com.yju.team2.seilomun.dto.*;
import jakarta.validation.Valid;
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
//        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
//                Map.of("Update", orderService.buyProduct(orderDto, customerId),
//                        "Message", "상품이 주문 되었습니다")));
    }

    @GetMapping("/buy")
    public ResponseEntity<ApiResponseJson> getBuyProduct(@RequestBody CartItemRequestDto cartItemRequestDto,
                                                         @AuthenticationPrincipal JwtUserDetails userDetail) {
        Long customerId = userDetail.getId();
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("주문페이지로 갑니다", orderService.getBuyProduct(cartItemRequestDto, customerId))));
    }}

//    @PostMapping("/toss")
//    public ResponseEntity<ApiResponseJson> requestTossPayment(@AuthenticationPrincipal JwtUserDetails userDetail
//                                                              , @