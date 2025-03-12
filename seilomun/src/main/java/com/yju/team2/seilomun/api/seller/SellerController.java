package com.yju.team2.seilomun.api.seller;


import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.service.SellerService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.SellerLoginDto;
import com.yju.team2.seilomun.dto.SellerRegisterDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
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
}
