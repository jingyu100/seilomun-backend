package com.yju.team2.seilomun.domain.customer.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.customer.dto.*;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.customer.repository.PointHistoryRepository;
import com.yju.team2.seilomun.domain.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @PostMapping
    public ApiResponseJson registerNewAccount(@Valid @RequestBody CustomerRegisterDto customerRegisterDto,
                                              BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        Customer customer = customerService.registerCustomer(customerRegisterDto);
        log.info("Account successfully registered with details: {}.", customer);

        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "email", customer.getEmail(),
                "username", customer.getName()
        ));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponseJson> getCustomerFavorite(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long customerId = userDetails.getId();
        FavoritePaginationDto favorites = customerService.getFavorite(customerId, page, size);

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "favorites", favorites.getFavorites(),
                "hasNext", favorites.isHasNext(),
                "totalElements", favorites.getTotalElements(),
                "message", "즐겨찾기 목록이 조회되었습니다."
        )));
    }

    @PostMapping("/favorites/{sellerId}")
    public ResponseEntity<ApiResponseJson> customerFavorite(@Valid @PathVariable Long sellerId,
                                                            @AuthenticationPrincipal JwtUserDetails userDetails) {

        try {
            String email = userDetails.getEmail();
            boolean isAdd = customerService.setFavorite(email, sellerId);

            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("message", isAdd ? "즐겨찾기가 추가되었습니다" : "즐겨찾기가 취소되었습니다",
                            "isAdd", isAdd,
                            "사용자", email,
                            "sellerId", sellerId)));
        } catch (Exception e) {
            log.error("매장 즐겨찾기 토글 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 즐겨찾기 토글 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/favorites/{favoriteId}")
    public ResponseEntity<ApiResponseJson> customerFavoriteDelete(@PathVariable Long favoriteId,
                                                                  @AuthenticationPrincipal JwtUserDetails userDetails) {
        try {
            String email = userDetails.getEmail();
            customerService.favoriteDelete(email, favoriteId);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "즐겨찾기가 취소 되었습니다",
                            "사용자", email)));
        } catch (Exception e) {
            log.error("매장 즐겨찾기 취소 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 즐겨찾기 취소 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/wishes")
    public ResponseEntity<ApiResponseJson> getCustomerWishes(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long customerId = userDetails.getId();
        WishPaginationDto wishes = customerService.getWishedProducts(customerId, page, size);

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "wishes", wishes.getWishes(),
                "hasNext", wishes.isHasNext(),
                "totalElements", wishes.getTotalElements(),
                "message", "위시리스트가 조회되었습니다."
        )));
    }

    // 상품id
    @PostMapping("/wishes/{productId}")
    public ResponseEntity<ApiResponseJson> customerWishes(@Valid @PathVariable Long productId,
                                                          @AuthenticationPrincipal JwtUserDetails userDetails) {

        try {
            String email = userDetails.getEmail();
            boolean isAdd = customerService.setWishes(email, productId);

            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("message", isAdd ? "상품 좋아요가 추가되었습니다" : "상품 좋아요가 취소되었습니다",
                            "isAdd", isAdd,
                            "사용자", email,
                            "productId", productId)));
        } catch (Exception e) {
            log.error("상품 좋아요 토글 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 좋아요 토글 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    // 좋아요 삭제
    @DeleteMapping("/wishes/{wishId}")
    public ResponseEntity<ApiResponseJson> customerWishesDelete(@Valid @PathVariable Long wishId,
                                                                @AuthenticationPrincipal JwtUserDetails userDetails) {
        try {
            String email = userDetails.getEmail();
            customerService.wishDelete(email, wishId);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "상품 좋아요가 취소 되었습니다",
                            "사용자", email)));
        } catch (Exception e) {
            log.error("상품 좋아요 취소 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 좋아요 취소 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    // 소비자 닉네임 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponseJson> getCustomer(@AuthenticationPrincipal JwtUserDetails userDetails) {
        String username = userDetails.getUsername();
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of("username", username))));
    }

    //소비자 정보 조회
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponseJson> getCustomerPage(@AuthenticationPrincipal JwtUserDetails userDetails) {
        LocalUserViewDto customer = customerService.getLocalUserDto(userDetails.getId());

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "customer", customer
        ))));
    }

    @PostMapping("/mypage/password")
    public ResponseEntity<ApiResponseJson> passwordValid(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                         @RequestBody PasswordValidDto passwordValidDto) {
        Long id = userDetails.getId();

        customerService.localUserPasswordValid(id, passwordValidDto);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
           "Message","비밀번호가 일치합니다"
        ))));
    }

    //소비자 정보 수정
    @PutMapping("/mypage/local")
    public ResponseEntity<ApiResponseJson> updateLocalCustomer(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                               @RequestBody @Valid LocalUserUpdateRequest request) {
        Long id = userDetails.getId();
        log.info("사용자의 정보 : {}", request.toString());
        customerService.localUserUpdateDto(id, request.getUpdateDto(), request.getPasswordChangeDto());

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "Message", "사용자 정보가 수정되었습니다."
        ))));
    }

    //소비자 정보 수정
    @PutMapping("/mypage/social")
    public ResponseEntity<ApiResponseJson> updateSocialCustomer(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                                @RequestBody @Valid SocialUserUpdateDto updateDto) {

        Long customerId = userDetails.getId();
        customerService.socialUserUpdateDto(customerId, updateDto);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "Message", "사용자 정보가 수정되었습니다."
        ))));
    }

    // 소비자 정보 수정
    @GetMapping
    public ResponseEntity<ApiResponseJson> getCustomers(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Customer customer = customerService.getUserDetailsByCustomerId(userDetails.getId());
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "customer", customer
        ))));
    }

    // 주문목록 보기
    @GetMapping("/orders")
    public ResponseEntity<ApiResponseJson> getOrders(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        OrderPaginationDto orders = customerService.getOrderList(userDetails.getId(), page, size);

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "orders", orders.getOrders(),
                "hasNext", orders.isHasNext(),
                "totalElements", orders.getTotalElements(),
                "message", "주문 목록이 조회되었습니다."
        )));
    }

    // 주문 상세 보기
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponseJson> getOrdersDetails(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "주문 상세 보기", customerService.getOrderDetail(userDetails.getId(), orderId)
        ))));
    }


    //문자인증
    @PostMapping("/verificationCode")
    public ResponseEntity<ApiResponseJson> sendVerificationCode(@RequestBody CustomerRegisterDto registerDto) {
        customerService.sendValidationCode(registerDto.getPhone());
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
            "Message","인증 번호가 발생되었습니다"
        ))));
    }
    


    // 포인트 내역 조회 (무한스크롤)
    @GetMapping("/points")
    public ResponseEntity<ApiResponseJson> getPointHistory(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long customerId = userDetails.getId();
        PointHistoryPaginationDto pointHistory = customerService.getPointHistory(customerId, page, size);

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "pointHistory", pointHistory.getPointHistories(),
                "currentPoints", pointHistory.getCurrentPoints(),
                "hasNext", pointHistory.isHasNext(),
                "totalElements", pointHistory.getTotalElements(),
                "message", "포인트 내역이 조회되었습니다."
        )));
    }
}
