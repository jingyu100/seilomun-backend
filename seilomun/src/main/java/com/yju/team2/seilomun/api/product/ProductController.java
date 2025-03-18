package com.yju.team2.seilomun.api.product;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.ProductDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    //상품 목록 조회
//    @GetMapping("/list")
//    public ResponseEntity<ApiResponseJson> getAllProducts() {
//        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
//                Map.of("Products", productService.getAllProducts(),
//                        "Message", "상품 리스트가 조회되었습니다")));
//    }

    //상품 상세 조회
    @GetMapping("/list/{id}")
    public ResponseEntity<ApiResponseJson> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Products", productService.getProductById(id),
                        "Message", "상품 상세 조회가 되었습니다")));
    }

    //상품 등록
    @PostMapping()
    public ResponseEntity<ApiResponseJson> createProductDto(@RequestBody ProductDto productDto,
                                                            BindingResult bindingResult,
                                                            @AuthenticationPrincipal JwtUserDetails userDetail) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        if (!userDetail.isSeller()) {
            throw new SecurityException("판매자만 상품을 등록할 수 있습니다.");
        }
        try {
            String sellerEmail = userDetail.getEmail();
            log.info("상품 등록 요청: 판매자 이메일 {}", sellerEmail);


            return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                    Map.of("Create", productService.createProductDto(productDto, sellerEmail),
                            "Message", "상품 등록이 되었습니다")));
        } catch (Exception e) {
            log.error("상품등록중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("상품 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    //상품 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponseJson> updateProductDto(@PathVariable Long id, @RequestBody ProductDto productDto,
                                                            @AuthenticationPrincipal JwtUserDetails userDetail) {
       String sellerEmail = userDetail.getEmail();

       return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
               Map.of("Update",productService.updateProductDto(id,productDto,sellerEmail),
                       "Message","상품이 수정 되었습니다")));
    }

    // 상품 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseJson> deleteProductDto(@PathVariable Long id
                                                            //BindingResult bindingResult
            , @AuthenticationPrincipal JwtUserDetails userDetail) {
       // if (bindingResult.hasErrors()) {
         //   throw new IllegalArgumentException("잘못된 요청입니다.");
        //}
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

}
