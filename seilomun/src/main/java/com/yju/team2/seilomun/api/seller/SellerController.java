package com.yju.team2.seilomun.api.seller;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.service.SellerService;
import com.yju.team2.seilomun.dto.*;
import com.yju.team2.seilomun.util.CookieUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
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

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponseJson> sellerLogin(@Valid @RequestBody SellerLoginDto sellerLoginDto,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        Map<String, String> tokens = sellerService.sellerLogin(sellerLoginDto);
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

    // 매장 정보 수정
    @PutMapping
    public ApiResponseJson updateSellerInformation(@Valid @RequestBody SellerInformationDto sellerInformationDto,
                                                   BindingResult bindingResult,
                                                   @AuthenticationPrincipal JwtUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        String email = userDetails.getEmail();

        try {
            Seller seller = sellerService.updateSellerInformation(email, sellerInformationDto);
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

}
