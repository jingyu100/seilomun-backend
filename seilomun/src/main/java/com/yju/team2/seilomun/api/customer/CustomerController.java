package com.yju.team2.seilomun.api.customer;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.service.CustomerService;
import com.yju.team2.seilomun.dto.*;
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

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/customers")
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

    @PostMapping("/customer/login")
    public ResponseEntity<ApiResponseJson> customerLogin(@Valid @RequestBody CustomerLoginDto customerLoginDto,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        String token = customerService.customerLogin(customerLoginDto);
        log.info("Account successfully login with token: {}.", token);

        ResponseCookie cookie = getResponseCookie(token);

        String email = customerLoginDto.getEmail();
        Customer customer = new Customer();
        try {
            customer = customerService.findByEmail(email);
            log.info("Customer found with email: {}.", customer);
        }
        catch (Exception e) {
            log.info("Customer not found with email: {}.", email);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString()) // 쿠키를 응답 헤더에 추가
                .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                        "token", token,
                        "nickName", customer.getNickname(),
                        "message", "로그인 성공"
                )));
    }

    // 쿠키에 토큰 삽입
    private static ResponseCookie getResponseCookie(String token) {
        // HttpOnly & Secure 쿠키 설정
        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .httpOnly(true)  // JavaScript에서 접근 불가능
                .secure(true)    // HTTPS에서만 전송
                .sameSite("None") // CSRF 방지를 위한 SameSite 설정
                /*
                Strict = 다른 사이트에서 요청할 때 쿠키가 전송되지 않음
                Lax = GET 요청 같은 안전한 요청에서는 쿠키 전송 가능
                None = 크로스 사이트 요청에서도 쿠키 사용 가능(HTTPS 필수)
                * */
                .path("/")       // 모든 경로에서 쿠키 사용 가능
                .maxAge(30 * 60 * 4) // 2시간 유지
                .build();
        return cookie;
    }

    @PostMapping("/customers/favorite/{id}")
    public ResponseEntity<ApiResponseJson> customerFavorite(@Valid @PathVariable Long id,
                                                            @AuthenticationPrincipal JwtUserDetails userDetails) {

        try {
            String email = userDetails.getEmail();
            customerService.favorite(email,id);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "즐겨찾기가 되었습니다",
                            "사용자",email)));
        } catch (Exception e) {
            log.error("매장 즐겨찾기 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 즐겨찾기 중 에러가 발생했습니다: " + e.getMessage());
        }
    }
    @DeleteMapping("/customers/favorite")
    public ResponseEntity<ApiResponseJson> customerFavoriteDelete(@Valid @RequestBody CustomerFavoriteDto customerFavoriteDto,
                                                             BindingResult bindingResult,
                                                            @AuthenticationPrincipal JwtUserDetails userDetails) {

        if (bindingResult.hasErrors()){
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
        try {
            String email = userDetails.getEmail();
            customerService.favoriteDelete(email,customerFavoriteDto.getId());
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "즐겨찾기가 취소 되었습니다",
                            "사용자",email)));
        } catch (Exception e) {
            log.error("매장 즐겨찾기 취소 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 즐겨찾기 취소 중 에러가 발생했습니다: " + e.getMessage());
        }
    }
    @PostMapping("/customers/wishes/{id}")
    public ResponseEntity<ApiResponseJson> customerWishes(@Valid @PathVariable Long id,
                                                            @AuthenticationPrincipal JwtUserDetails userDetails) {

        try {
            String email = userDetails.getEmail();
            customerService.wishes(email,id);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "상품 좋아요가 되었습니다",
                            "사용자",email)));
        } catch (Exception e) {
            log.error("상품 좋아요 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 좋아요 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/customers/wishes")
    public ResponseEntity<ApiResponseJson> customerWishesDelete(@Valid @RequestBody CustomerWishesDto customerWishesDto,
                                                                BindingResult bindingResult,
                                                                @AuthenticationPrincipal JwtUserDetails userDetails) {
        if (bindingResult.hasErrors()){
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        try {
            String email = userDetails.getEmail();
            customerService.wishDelete(email,customerWishesDto.getId());
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "상품 좋아요가 취소 되었습니다",
                            "사용자",email)));
        } catch (Exception e) {
            log.error("상품 좋아요 취소 중 에러 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 좋아요 취소 중 에러가 발생했습니다: " + e.getMessage());
        }
    }
}
