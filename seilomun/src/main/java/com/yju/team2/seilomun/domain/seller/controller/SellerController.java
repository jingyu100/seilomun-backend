package com.yju.team2.seilomun.domain.seller.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.seller.dto.SellerInformationDto;
import com.yju.team2.seilomun.domain.seller.dto.SellerRegisterDto;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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

    // 가게 상세 조회
    @GetMapping("/{sellerId}")
    public ResponseEntity<ApiResponseJson> getSellerInformation(@PathVariable Long sellerId) {
        SellerInformationDto sellerInformationDto = sellerService.getSellerById(sellerId);

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK, Map.of(
                "seller", sellerInformationDto,
                "Message","가게 정보가 조회되었습니다."
        )));
    }
}
