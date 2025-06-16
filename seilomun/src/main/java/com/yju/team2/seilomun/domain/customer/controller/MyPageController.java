package com.yju.team2.seilomun.domain.customer.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.customer.dto.LocalUserUpdateRequest;
import com.yju.team2.seilomun.domain.customer.dto.LocalUserViewDto;
import com.yju.team2.seilomun.domain.customer.dto.PasswordValidDto;
import com.yju.team2.seilomun.domain.customer.dto.SocialUserUpdateDto;
import com.yju.team2.seilomun.domain.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RequestMapping("/api/customers/mypage")
@RestController
@Slf4j
@RequiredArgsConstructor
public class MyPageController {

    private final CustomerService customerService;

    //소비자 정보 조회
    @GetMapping
    public ResponseEntity<ApiResponseJson> getCustomerPage(@AuthenticationPrincipal JwtUserDetails userDetails) {
        LocalUserViewDto customer = customerService.getLocalUserDto(userDetails.getId());

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "customer", customer
        ))));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponseJson> passwordValid(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                         @RequestBody PasswordValidDto passwordValidDto) {
        Long id = userDetails.getId();

        customerService.localUserPasswordValid(id, passwordValidDto);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "Message","비밀번호가 일치합니다"
        ))));
    }

    @PutMapping("/local/profile")
    public ResponseEntity<ApiResponseJson> localProfileUpdate(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                              @RequestPart(value = "profileImage",required = false) MultipartFile multipartFile) {
        Long id = userDetails.getId();
        System.out.println("프로필 이미지 업로드 요청 받음, userId = " + id);
        System.out.println("파일명: " + multipartFile.getOriginalFilename());


        String imageUrl = customerService.localProfile(id,multipartFile);
        System.out.println("업로드 완료, imageUrl = " + imageUrl);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "Message","프로필 수정이 완료되었습니다.",
                "profileImageUrl",imageUrl
        ))));

    }

    //소비자 정보 수정
    @PutMapping("/local")
    public ResponseEntity<ApiResponseJson> updateLocalCustomer(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                               @RequestBody LocalUserUpdateRequest request) {
        Long id = userDetails.getId();

        customerService.localUserUpdateDto(id, request.getUpdateDto(), request.getPasswordChangeDto());

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "Message", "사용자 정보가 수정되었습니다."
        ))));
    }

    //소비자 정보 수정
    @PutMapping("/social")
    public ResponseEntity<ApiResponseJson> updateSocialCustomer(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                                @RequestBody @Valid SocialUserUpdateDto updateDto) {

        Long customerId = userDetails.getId();
        customerService.socialUserUpdateDto(customerId, updateDto);

        return ResponseEntity.ok((new ApiResponseJson(HttpStatus.OK, Map.of(
                "Message", "사용자 정보가 수정되었습니다."
        ))));
    }

}
