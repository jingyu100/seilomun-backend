package com.yju.team2.seilomun.api.customer;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.service.CustomerService;
import com.yju.team2.seilomun.dto.*;
import com.yju.team2.seilomun.util.CookieUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

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

    @PostMapping("/login")
    public ResponseEntity<ApiResponseJson> customerLogin(@Valid @RequestBody CustomerLoginDto customerLoginDto,
                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        Map<String, String> tokens = customerService.customerLogin(customerLoginDto);
        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        // 액세스 토큰용 쿠키 설정 (2시간 만료)
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(accessToken);

        // 리프레시 토큰용 쿠키 설정 (14일 만료)
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                        "message", "로그인 성공"
                )));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponseJson> getCustomerFavorite(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long customerId = userDetails.getId();
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "즐겨찾기", customerService.getFavorite(customerId)
        ))));
    }

    @PostMapping("/favorites/{id}")
    public ResponseEntity<ApiResponseJson> customerFavorite(@Valid @PathVariable Long id,
                                                            @AuthenticationPrincipal JwtUserDetails userDetails) {

        try {
            String email = userDetails.getEmail();
            customerService.setFavorite(email, id);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "즐겨찾기가 되었습니다",
                            "사용자", email)));
        } catch (Exception e) {
            log.error("매장 즐겨찾기 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 즐겨찾기 중 에러가 발생했습니다: " + e.getMessage());
        }
    }
    @DeleteMapping("/favorites/{id}")
    public ResponseEntity<ApiResponseJson> customerFavoriteDelete(@PathVariable Long id,
                                                                  BindingResult bindingResult,
                                                                  @AuthenticationPrincipal JwtUserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
        try {
            String email = userDetails.getEmail();
            customerService.favoriteDelete(email, id);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "즐겨찾기가 취소 되었습니다",
                            "사용자", email)));
        } catch (Exception e) {
            log.error("매장 즐겨찾기 취소 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 즐겨찾기 취소 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/wishes")
    public ResponseEntity<ApiResponseJson> getCustomerWishes(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long customerId = userDetails.getId();
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "즐겨찾기", customerService.getWishedProducts(customerId)
        ))));
    }

    // 상품id
    @PostMapping("/wishes/{id}")
    public ResponseEntity<ApiResponseJson> customerWishes(@Valid @PathVariable Long id,
                                                          @AuthenticationPrincipal JwtUserDetails userDetails) {

        try {
            String email = userDetails.getEmail();
            customerService.setwishes(email, id);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "상품 좋아요가 되었습니다",
                            "사용자", email)));
        } catch (Exception e) {
            log.error("상품 좋아요 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 좋아요 중 에러가 발생했습니다: " + e.getMessage());
        }
    }
    // 상품id
    @DeleteMapping("/wishes/{id}")
    public ResponseEntity<ApiResponseJson> customerWishesDelete(@Valid @PathVariable Long id,
                                                                BindingResult bindingResult,
                                                                @AuthenticationPrincipal JwtUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        try {
            String email = userDetails.getEmail();
            customerService.wishDelete(email, id);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "상품 좋아요가 취소 되었습니다",
                            "사용자", email)));
        } catch (Exception e) {
            log.error("상품 좋아요 취소 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 좋아요 취소 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

}
