package com.yju.team2.seilomun.domain.product.controller;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.domain.seller.service.SellerService;
import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.product.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final SellerService sellerService;

    //상품 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseJson> getProductById(@PathVariable Long id) {

        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Products", productService.getProductById(id),
                        "Message", "상품 상세 조회가 되었습니다")));
    }

    // 상품 등록
    @PostMapping
    public ResponseEntity<ApiResponseJson> createProductDto(@RequestPart ProductDto productDto,
                                                            BindingResult bindingResult,
                                                            @AuthenticationPrincipal JwtUserDetails userDetail,
                                                            @RequestPart(value = "photoImages",required = false) List<MultipartFile> file) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        try {
            String sellerEmail = userDetail.getEmail();
            log.info("상품 등록 요청: 판매자 이메일 {}", sellerEmail);


            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Create", productService.createProductDto(productDto, sellerEmail,file),
                            "Message", "상품 등록이 되었습니다")));
        } catch (Exception e) {
            log.error("상품등록중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    //상품 수정
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponseJson> updateProductDto(@PathVariable Long productId, @RequestPart("productDto") ProductDto productDto,
                                                            @AuthenticationPrincipal JwtUserDetails userDetail,
                                                            @RequestPart(value= "productPhotos",required = false) List<MultipartFile> productPhotos) {
        String sellerEmail = userDetail.getEmail();

        try {
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Update", productService.updateProductDto(productId, productDto, sellerEmail,productPhotos),
                            "Message", "상품이 수정 되었습니다")));
        } catch (Exception e) {
            log.error("상품 정보 업데이트 중 오류 발생 : {}",e.getMessage());
            throw new RuntimeException("상품 정보 업데이트 중 오류가 발생했습니다."+ e.getMessage());
        }

    }

    // 상품 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseJson> deleteProductDto(@PathVariable Long id,
                                                            @AuthenticationPrincipal JwtUserDetails userDetail) {
        try {
            String sellerEmail = userDetail.getEmail();
            productService.deleteProduct(id, sellerEmail);
            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Message", "상품이 삭제 되었습니다")));
        } catch (Exception e) {
            log.error("상품등록중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/seller/{sellerId}")
    public List<ProductDto> getProducts(@PathVariable Long sellerId) {
        return productService.getProducts(sellerId);
    }
}
