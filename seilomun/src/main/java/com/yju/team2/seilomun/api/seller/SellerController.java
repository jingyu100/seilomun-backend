package com.yju.team2.seilomun.api.seller;


import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.service.SellerService;
import com.yju.team2.seilomun.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SellerController {
    private final SellerService sellerService;

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
    public ApiResponseJson sellerLogin(@Valid @RequestBody SellerLoginDto sellerLoginDto,
                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
        String taken = sellerService.sellerLogin(sellerLoginDto);
        log.info("Account successfully login with details: {}.", taken);

        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "taken", taken,
                "mesaage","로그인 성공"
        ));
    }

//    토큰 못가져와서 이거 잠시 주석처리
//    @PostMapping("/seller/information")
//    public ApiResponseJson updateSellerInformation(@Valid @RequestBody SellerInformationDto sellerInformationDto,
//                                                   BindingResult bindingResult) {
//        if (bindingResult.hasErrors()) {
//            throw new IllegalArgumentException("잘못된 요청입니다.");
//        }
//
//        // 현재 인증된 사용자의 이메일 가져오기
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//
//        Seller seller = sellerService.updateSellerInformation(email, sellerInformationDto);
//        log.info("판매자 매장 정보가 성공적으로 업데이트되었습니다: {}", email);
//
//        return new ApiResponseJson(HttpStatus.OK, Map.of(
//                "storeName", seller.getStoreName(),
//                "message", "매장 정보가 성공적으로 업데이트되었습니다."
//        ));
//    }
    //임시로 매장정보 수정
    @PostMapping("/seller/testInformation")
    public ApiResponseJson testSellerInformation(@Valid @RequestBody SellerInformationDto sellerInformationDto,
                                                 BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        // 테스트용 이메일 post맨으로 보낸거랑 같아야함
        String testEmail = "aaa@naver.com";

        Seller seller = sellerService.updateSellerInformation(testEmail, sellerInformationDto);
        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "storeName", seller.getStoreName(),
                "message", "매장 정보가 성공적으로 업데이트되었습니다."
        ));

    }
    @PostMapping("/seller/testInsertDeliveryFee")
    public ApiResponseJson testInsertDeliveryFee(@Valid @RequestBody DeliveryFeeDto deliveryFeeDto,
                                                 BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
        // 테스트용 이메일 post맨으로 보낸거랑 같아야함
        String testEmail = "aaa@naver.com";
        Seller seller = sellerService.insertDeliveryFee(testEmail, deliveryFeeDto);

        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "storeName", seller.getStoreName(),
                "message", "매장에 배달비가 성공적으로 업데이트 되었습니다."
        ));

    }
}
