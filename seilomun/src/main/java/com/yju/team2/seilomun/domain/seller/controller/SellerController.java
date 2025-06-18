package com.yju.team2.seilomun.domain.seller.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.seller.dto.*;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/sellers")
public class SellerController {

    private final SellerService sellerService;

    // valid 어노테이션은 유효성 검사
    // 회원가입
    @PostMapping
    public ApiResponseJson sellerRegister(@Valid @RequestBody SellerRegisterDto sellerRegisterDto,
                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        Seller seller = sellerService.sellerRegister(sellerRegisterDto);
        log.info("Account successfully registered with details: {}.", seller);

        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "email", seller.getEmail(),
                "username", seller.getStoreName()
        ));
    }

    // 매장 정보 수정
    @PutMapping
    public ApiResponseJson updateSellerInformation(@Valid @RequestPart("sellerInformationDto") SellerInformationDto sellerInformationDto,
                                                   BindingResult bindingResult,
                                                   @AuthenticationPrincipal JwtUserDetails userDetails,
                                                   @RequestPart(value = "storeImage",required = false) List<MultipartFile> storeImage,
                                                   @RequestPart(value = "notificationImage",required = false) List<MultipartFile> notificationImage) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        String email = userDetails.getEmail();

        try {
            Seller seller = sellerService.updateSellerInformation(email, sellerInformationDto,storeImage,notificationImage);
            log.info("판매자 매장 정보가 성공적으로 업데이트되었습니다: {}", email);

            return new ApiResponseJson(HttpStatus.OK, Map.of(
                    "storeName", seller.getStoreName(),
                    "message", "매장 정보와 배달비가 성공적으로 업데이트되었습니다."
            ));
        } catch (Exception e) {
            log.error("매장 정보 업데이트 중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 가게 상세 조회
    @GetMapping("/{sellerId}")
    public ResponseEntity<ApiResponseJson> getSellerInformation(@PathVariable Long sellerId) {
        SellerInformationResponseDto sellerInformationDto = sellerService.getSellerById(sellerId);

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "seller", sellerInformationDto,
                "Message","가게 정보가 조회되었습니다."
        )));
    }


    // 판매자가 자신의 영업 상태 변경
    @PutMapping("/me/status")
    public ResponseEntity<ApiResponseJson> updateMyStatus(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestBody SellerStatusUpdateDto request) {

        if (!"SELLER".equals(userDetails.getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseJson(HttpStatus.FORBIDDEN,
                            Map.of("error", "판매자만 접근 가능합니다")));
        }

        try {
            sellerService.updateSellerStatus(userDetails.getEmail(), request.getIsOpen());

            String statusMessage = getStatusMessage(request.getIsOpen());

            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                    "message", "영업 상태가 " + statusMessage + "로 변경되었습니다",
                    "status", request.getIsOpen()
            )));
        } catch (Exception e) {
            log.error("판매자 상태 변경 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseJson(HttpStatus.INTERNAL_SERVER_ERROR,
                            Map.of("error", "상태 변경에 실패했습니다")));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseJson> getMySellers(@AuthenticationPrincipal JwtUserDetails userDetails) {
        String storeName = userDetails.getUsername();
        SellerInformationResponseDto sellerInformationDto = sellerService.getSellerById(userDetails.getId());
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "storeName",storeName,
                "sellerInformationDto", sellerInformationDto
        )));
    }

    // 소비자 정보 수정
    @GetMapping
    public ResponseEntity<ApiResponseJson> getSellers(@AuthenticationPrincipal JwtUserDetails userDetails) {
        SellerInforResDto seller = sellerService.getUserDetailsBySellerId(userDetails.getId());
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "seller", seller
        ))));
    }

    // 판매자용 주문 상세 페이지 (이거 불러오고 아래에다가 주문 수락 거절 버튼 만드세요.)
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponseJson> getOrderDetails(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "orderDetail", sellerService.getOrderDetail(userDetails.getId(), orderId),
                "message", "주문 상세 조회가 완료되었습니다."
        )));
    }

    // 판매자용 주문 목록 조회
    @GetMapping("/orders")
    public ResponseEntity<ApiResponseJson> getOrders(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "orders", sellerService.getOrderList(userDetails.getId(), page, size),
                "message", "주문 목록 조회가 완료되었습니다."
        )));
    }

    // 판매자용 환불 상세 페이지
    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<ApiResponseJson> getRefundDetails(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long refundId) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "refundDetail", sellerService.getRefundDetail(userDetails.getId(), refundId),
                "message", "환불 상세 조회가 완료되었습니다."
        )));
    }

    // 주문번호 기반 주문 상세 조회
    @GetMapping("/orders/number/{orderNumber}")
    public ResponseEntity<ApiResponseJson> getOrderDetailsByOrderNumber(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "orderDetail", sellerService.getOrderDetailByOrderNumber(userDetails.getId(), orderNumber),
                "message", "주문 상세 조회가 완료되었습니다."
        )));
    }


    private String getStatusMessage(Character isOpen) {
        switch (isOpen) {
            case '1': return "영업중";
            case '0': return "영업종료";
            case '2': return "브레이크타임";
            default: return "알 수 없음";
        }
    }
}
