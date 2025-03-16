package com.yju.team2.seilomun.api.product;

import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.ProductDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController{

    private final ProductService productService;

    //상품 목록 조회
    @GetMapping("/list")
    public ResponseEntity<ApiResponseJson> getAllProducts(HttpSession httpSession) {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Products",productService.getAllProducts(),
                "Message","상품 리스트가 조회되었습니다")));
    }

    //상품 상세 조회
    @GetMapping("/list/{id}")
    public ResponseEntity<ApiResponseJson> getProductById(@PathVariable Long id)
    {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Products",productService.getProductById(id),
                        "Message","상품 상세 조회가 되었습니다")));
    }

    //상품 등록
    @PostMapping("/create")
    public ResponseEntity<ApiResponseJson> createProductDto(@RequestBody ProductDto productDto)
    {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                Map.of("Create",productService.createProductDto(productDto),
                        "Message","상품 등록이 되었습니다")));
    }

    //상품 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponseJson> updateProductDto(@PathVariable Long id,@RequestBody ProductDto productDto)
    {
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
            Map.of("Update",productService.updateProductDto(id,productDto),
                    "Message","상품 수정이 완료되었습니다")));
    }

    // 상품 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseJson> deleteProductDto(@PathVariable Long id)
    {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponseJson(HttpStatus.OK,
                        Map.of("Message","상품이 삭제 되었습니다")));
    }

}
