package com.yju.team2.seilomun.api.product;

import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController{

    private final ProductService productService;

    //상품 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<ProductDto>> getAllProduct() {
        return ResponseEntity.ok(productService.getAllProudcts());
    }

    //상품 상세 조회
    @GetMapping("/list/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id)
    {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    //상품 등록
    @PostMapping("/create")
    public ResponseEntity<ProductDto> createProudct(@RequestBody ProductDto productDto)
    {
        return ResponseEntity.ok(productService.createProductDto(productDto));
    }
}
