package com.yju.team2.seilomun.api.order;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.order.service.OrderService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public ResponseEntity<ApiResponseJson> buyProduct(@RequestBody OrderDto orderDto,
                                                      @AuthenticationPrincipal JwtUserDetails userDetail) {
        Long customerId = userDetail.getId();
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Update",         orderService.buyProduct(orderDto, customerId),
                        "Message", "상품이 주문 되었습니다")));
    }

}
