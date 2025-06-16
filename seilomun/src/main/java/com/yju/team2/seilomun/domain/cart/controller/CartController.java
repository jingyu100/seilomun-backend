package com.yju.team2.seilomun.domain.cart.controller;

import com.yju.team2.seilomun.domain.auth.dto.CartItemRequestDto;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.cart.service.CartService;
import com.yju.team2.seilomun.common.ApiResponseJson;
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
@RequestMapping("/api/carts")
@Slf4j
public class CartController {

    private final CartService cartService;

    // 장바구니 상품 조회
    @GetMapping
    public ResponseEntity<ApiResponseJson> getCart(@AuthenticationPrincipal JwtUserDetails user) {
        log.info("장바구니 조회 요청: userId={}", user.getId());

        Map<Long, Integer> cartItems = cartService.getCartItems(user.getId());

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of(
                        "products", cartItems,
                        "totalItems", cartItems.size(),
                        "message", "장바구니 조회가 완료되었습니다"
                )
        ));
    }


    // 장바구니 상품 추가
    @PostMapping
    public ResponseEntity<ApiResponseJson> addToCart(
            @Valid @RequestBody CartItemRequestDto request,
            @AuthenticationPrincipal JwtUserDetails user) {

        log.info("장바구니 상품 추가 요청: userId={}, productId={}, quantity={}",
                user.getId(), request.getProductId(), request.getQuantity());

        try {
            int newQuantity = cartService.addToCart(user.getId(), request.getProductId(), request.getQuantity());

            return ResponseEntity.ok(new ApiResponseJson(
                    HttpStatus.OK,
                    Map.of(
                            "message", "상품이 장바구니에 추가되었습니다",
                            "productId", request.getProductId(),
                            "newQuantity", newQuantity
                    )
            ));
        } catch (IllegalArgumentException e) {
            // 비즈니스 규칙 위반 (다른 판매자 상품 등)
            log.warn("장바구니 추가 실패 - 비즈니스 규칙 위반: userId={}, productId={}, reason={}",
                    user.getId(), request.getProductId(), e.getMessage());

            return ResponseEntity.badRequest().body(new ApiResponseJson(
                    HttpStatus.BAD_REQUEST,
                    Map.of(
                            "error", "BUSINESS_RULE_VIOLATION",
                            "message", e.getMessage(),
                            "productId", request.getProductId()
                    )
            ));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("존재하지 않는 상품")) {
                // 상품 없음
                log.warn("장바구니 추가 실패 - 상품 없음: userId={}, productId={}",
                        user.getId(), request.getProductId());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseJson(
                        HttpStatus.NOT_FOUND,
                        Map.of(
                                "error", "PRODUCT_NOT_FOUND",
                                "message", e.getMessage(),
                                "productId", request.getProductId()
                        )
                ));
            } else if (e.getMessage().contains("재고 부족")) {
                // 재고 부족
                log.warn("장바구니 추가 실패 - 재고 부족: userId={}, productId={}",
                        user.getId(), request.getProductId());

                return ResponseEntity.badRequest().body(new ApiResponseJson(
                        HttpStatus.BAD_REQUEST,
                        Map.of(
                                "error", "INSUFFICIENT_STOCK",
                                "message", e.getMessage(),
                                "productId", request.getProductId()
                        )
                ));
            } else {
                // 기타 시스템 오류
                log.error("장바구니 추가 중 시스템 오류: userId={}, productId={}",
                        user.getId(), request.getProductId(), e);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseJson(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Map.of(
                                "error", "INTERNAL_SERVER_ERROR",
                                "message", "장바구니에 상품을 추가하는 중 오류가 발생했습니다"
                        )
                ));
            }
        } catch (Exception e) {
            // 예상치 못한 오류
            log.error("장바구니 추가 중 예상치 못한 오류: userId={}, productId={}",
                    user.getId(), request.getProductId(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseJson(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    Map.of(
                            "error", "UNEXPECTED_ERROR",
                            "message", "예상치 못한 오류가 발생했습니다"
                    )
            ));
        }
    }

    // 장바구니 상품 수량 업데이트
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponseJson> updateCartItemQuantity(
            @PathVariable(name = "productId") Long productId,
            @Valid @RequestBody CartItemRequestDto request,
            @AuthenticationPrincipal JwtUserDetails user) {

        log.info("장바구니 상품 수량 업데이트 요청: userId={}, productId={}, quantity={}",
                user.getId(), productId, request.getQuantity());

        // URL의 productId와 요청 본문의 productId가 일치하는지 확인
        if (!productId.equals(request.getProductId())) {
            return ResponseEntity.badRequest().body(new ApiResponseJson(
                    HttpStatus.BAD_REQUEST,
                    Map.of("message", "URL의 상품 ID와 요청 본문의 상품 ID가 일치하지 않습니다")
            ));
        }

        cartService.updateCartItemQuantity(user.getId(), productId, request.getQuantity());

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of(
                        "message", "장바구니 상품 수량이 업데이트되었습니다",
                        "productId", productId,
                        "quantity", request.getQuantity()
                )
        ));
    }

    // 장바구니 특정 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponseJson> removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal JwtUserDetails user) {

        log.info("장바구니 상품 제거 요청: userId={}, productId={}", user.getId(), productId);

        boolean removed = cartService.removeFromCart(user.getId(), productId);

        if (removed) {
            return ResponseEntity.ok(new ApiResponseJson(
                    HttpStatus.OK,
                    Map.of(
                            "productId", productId,
                            "message", "상품이 장바구니에서 제거되었습니다"
                    )
            ));
        } else {
            return ResponseEntity.ok(new ApiResponseJson(
                    HttpStatus.OK,
                    Map.of(
                            "productId", productId,
                            "message", "장바구니에서 제거할 상품이 존재하지 않습니다",
                            "removed", false
                    )
            ));
        }
    }

    // 장바구니 초기화
    @DeleteMapping
    public ResponseEntity<ApiResponseJson> clearCart(@AuthenticationPrincipal JwtUserDetails user) {
        log.info("장바구니 비우기 요청: userId={}", user.getId());

        cartService.clearCart(user.getId());

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of("message", "장바구니가 성공적으로 비워졌습니다")
        ));
    }
}