package com.yju.team2.seilomun.api.seller;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.seller.entity.DeliveryFee;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.service.SellerService;
import com.yju.team2.seilomun.dto.*;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SellerController {

    private final SellerService sellerService;
    private final JwtUtil jwtUtil;

    //valid 어노테이션은 유효성 검사
    @PostMapping("/seller/join")
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

    //로그인
    @PostMapping("/seller/login")
    public ResponseEntity<ApiResponseJson> sellerLogin(@Valid @RequestBody SellerLoginDto sellerLoginDto,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        String token = sellerService.sellerLogin(sellerLoginDto);
        log.info("Account successfully login with token: {}.", token);

        ResponseCookie cookie = getResponseCookie(token);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString()) // 쿠키를 응답 헤더에 추가
                .body(new ApiResponseJson(HttpStatus.OK, Map.of(
                        "token", token,
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

    // 쿠키에 토큰이 들어가있는지 확인
    @PostMapping("api/test")
    public ApiResponseJson checkSellerInformation(HttpServletRequest request) {

        // 모든 쿠키 가져오기
        Cookie[] cookies = request.getCookies();
        String token = null;

        // 디버그용 로그 추가
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("쿠키 이름: {}, 값: {}", cookie.getName(), cookie.getValue());
                if ("Authorization".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        } else {
            log.info("쿠키가 없습니다");
        }

        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        try {
            String email = jwtUtil.extractUsername(token);
            log.info("추출된 이메일: {}", email);

            return new ApiResponseJson(HttpStatus.OK, Map.of(
                    "sellerEmail", email
            ));
        } catch (Exception e) {
            throw new IllegalArgumentException("오류가 발생했습니다 : " + e.getMessage());
        }
    }

    //매장 정보 수정
    @PostMapping("/seller/information")
    public ApiResponseJson updateSellerInformation(@Valid @RequestBody SellerInformationDto sellerInformationDto,
                                                   BindingResult bindingResult,
                                                   @AuthenticationPrincipal JwtUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        // 판매자 권한 확인
        if (!userDetails.isSeller()) {
            throw new SecurityException("판매자만 접근할 수 있습니다.");
        }

        // 현재 인증된 사용자의 이메일 가져오기
        String email = userDetails.getEmail();

        // 현재 인증된 사용자의 이메일 가져오기
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();

        Seller seller = sellerService.updateSellerInformation(email, sellerInformationDto);
        log.info("판매자 매장 정보가 성공적으로 업데이트되었습니다: {}", email);

        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "storeName", seller.getStoreName(),
                "message", "매장 정보가 성공적으로 업데이트되었습니다."
        ));
    }
    // 매장 배달비 추가
    @PostMapping("/seller/insertDeliveryFee")
    public ApiResponseJson insertDeliveryFee(@Valid @RequestBody DeliveryFeeDto deliveryFeeDto,
                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Seller seller = sellerService.insertDeliveryFee(email, deliveryFeeDto);

            return new ApiResponseJson(HttpStatus.OK, Map.of(
                    "storeName", seller.getStoreName(),
                    "message", "매장에 배달비가 성공적으로 업데이트 되었습니다."
            ));
        } catch (Exception e) {
            log.error("매장 정보 업데이트 중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("매장 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    // 매장 배달비 수정
    @PostMapping("/seller/updateDeliveryFee")
    public ApiResponseJson updateDeliveryFee(@Valid @RequestBody DeliveryFeeDto deliveryFeeDto,
                                             BindingResult bindingResult,
                                             Authentication authentication) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        String email = authentication.getName();

        try{
            DeliveryFee deliveryFee = sellerService.updateDeliveryFee(email,deliveryFeeDto);
            return new ApiResponseJson(HttpStatus.OK, Map.of(
                    "deliveryTip", deliveryFee.getDeliveryTip(),
                    "message", "매장에 배달비가 성공적으로 업데이트 되었습니다."
            ));
        } catch (Exception e) {
            throw new IllegalArgumentException("매장에 배달비 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    // 매장 배달비 삭제
    @DeleteMapping("/seller/deleteDeliveryFee")
    public  ApiResponseJson deleteDeliveryFee(@Valid @RequestBody DeliveryFeeDto deliveryFeeDto,
                                              BindingResult bindingResult,
                                              Authentication authentication){
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        String email = authentication.getName();
        try{
            sellerService.deleteDeliveryFee(email,deliveryFeeDto);
            return new ApiResponseJson(HttpStatus.OK, Map.of(
                    "deliveryTip", deliveryFeeDto.getId(),
                    "message", "매장에 배달비가 성공적으로 삭제 되었습니다."
            ));
        } catch (Exception e) {
            throw new IllegalArgumentException("매장에 배달비 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
