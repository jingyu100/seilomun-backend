package com.yju.team2.seilomun.api.cart;

import com.yju.team2.seilomun.domain.auth.CartItemRequestDto;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.cart.service.CartService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/carts")
public class CartController {

    private final CartService cartService;

    // 장바구니 상품 조회
    @GetMapping
    public ResponseEntity<ApiResponseJson> getCart(@AuthenticationPrincipal JwtUserDetails user) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Products", cartService.getCartItems(user.getUserId()),
                        "Message", "장바구니 조회가 완료되었습니다")));
    }

    // 장바구니 상품 추가
    @PostMapping
    public ResponseEntity<ApiResponseJson> addToCart(@RequestBody CartItemRequestDto request,
                                                     @AuthenticationPrincipal JwtUserDetails user) {
        cartService.addToCart(user.getUserId(), request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Message", "해당 상품이 장바구니에 추가되었습니다")));
    }

    // 장바구니 특정 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponseJson> removeItem(@PathVariable Long productId,
                                                      @AuthenticationPrincipal JwtUserDetails user) {
        cartService.removeFromCart(user.getUserId(), productId);
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Message", "해당 상품이 삭제되었습니다")));
    }

    // 장바구니 초기화
    @DeleteMapping
    public ResponseEntity<ApiResponseJson> clearCart(@AuthenticationPrincipal JwtUserDetails user) {
        cartService.clearCart(user.getUserId());
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Message", "해당 유저의 장바구니가 초기화 되었습니다.")));
    }
}