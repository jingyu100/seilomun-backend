package com.yju.team2.seilomun.api.product;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.enums.ProductFilterType;
import com.yju.team2.seilomun.domain.product.enums.ProductSortType;
import com.yju.team2.seilomun.domain.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<ProductDocument>> searchProducts(
//            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "ALL") ProductFilterType filterType,
            @RequestParam(required = false, defaultValue = "LATEST") ProductSortType sortType
    ) {
        List<ProductDocument> results = productSearchService.advancedSearch(keyword, filterType, sortType);
        return ResponseEntity.ok(results);
    }

//    @GetMapping("/search/recent")
//    public ResponseEntity<List<ProductDocument>> searchRecentProducts(@RequestParam String keyword) {
//        List<ProductDocument> results = productSearchService.searchRecentProducts(keyword);
//        return ResponseEntity.ok(results);
//    }
//
//    @GetMapping("/search/expiring")
//    public ResponseEntity<List<ProductDocument>> searchExpiringProducts(@RequestParam String keyword) {
//        List<ProductDocument> results = productSearchService.searchExpiringProducts(keyword);
//        return ResponseEntity.ok(results);
//    }
}