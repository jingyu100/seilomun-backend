package com.yju.team2.seilomun.domain.customer.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.customer.dto.AddressRequestDto;
import com.yju.team2.seilomun.domain.customer.repository.AddressRepository;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {


    private final CustomerService customerService;

    //소비자 주소 조회
    @GetMapping
    public ResponseEntity<ApiResponseJson> getAddress(@AuthenticationPrincipal JwtUserDetails userDetails) {
        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "address", customerService.getAddresses(userDetails.getId())
        ))));
    }

    //소비자 주소 추가
    @PostMapping
    public ResponseEntity<ApiResponseJson> addAddress(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                      @RequestBody AddressRequestDto request) {

        customerService.addAddress(userDetails.getId(), request);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "message", "주소가 등록되었습니다"
        ))));
    }

    //소비자 주소 수정
    @PutMapping("{addressId}")
    public ResponseEntity<ApiResponseJson> updateAddress(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                         @PathVariable Long addressId,
                                                         @RequestBody AddressRequestDto request) {
        customerService.updateAddress(addressId, request);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "message", "주소가 수정되었습니다"
        ))));
    }

    //소비자 주소 삭제
    @DeleteMapping("{addressId}")
    public ResponseEntity<ApiResponseJson> deleteAddress(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                         @PathVariable Long addressId) {

        customerService.deleteAddress(addressId);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "message", "주소가 삭제되었습니다"
        ))));
    }
}
