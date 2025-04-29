package com.yju.team2.seilomun.domain.product.controller;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.enums.ProductFilterType;
import com.yju.team2.seilomun.domain.product.enums.ProductSortType;
import com.yju.team2.seilomun.domain.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping("/search")
    public ResponseEntity<Page<ProductDocument>> searchProducts(@RequestParam(required = false, defaultValue = "") String keyword,
                                                                @RequestParam(required = false, defaultValue = "ALL") ProductFilterType filterType,
                                                                @RequestParam(required = false, defaultValue = "LATEST") ProductSortType sortType,
                                                                @RequestParam(required = false, defaultValue = "0") int page,
                                                                @RequestParam(required = false, defaultValue = "10") int size) {
        Page<ProductDocument> results = productSearchService.advancedSearch(keyword, filterType, sortType, page, size);
        return ResponseEntity.ok(results);
    }

}