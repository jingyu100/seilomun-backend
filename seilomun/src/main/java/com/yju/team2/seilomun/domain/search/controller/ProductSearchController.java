package com.yju.team2.seilomun.domain.search.controller;

import com.yju.team2.seilomun.domain.product.dto.DiscountInfo;
import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.service.ProductDiscountService;
import com.yju.team2.seilomun.domain.search.dto.ProductSearchDto;
import com.yju.team2.seilomun.domain.search.enums.ProductFilterType;
import com.yju.team2.seilomun.domain.search.enums.ProductSortType;
import com.yju.team2.seilomun.domain.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductDiscountService productDiscountService;

    @GetMapping("/search")
    public ResponseEntity<Page<ProductSearchDto>> searchProducts(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId, // 카테고리 필터 추가
            @RequestParam(required = false, defaultValue = "ALL") ProductFilterType filterType,
            @RequestParam(required = false, defaultValue = "LATEST") ProductSortType sortType,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        Page<ProductDocument> results = productSearchService.searchProducts(
                keyword, categoryId, filterType, sortType, page, size);

        // ProductDocument를 ProductSearchDto로 변환하면서 status 필터링
        List<ProductSearchDto> dtoList = results.getContent().stream()
                .filter(productDocument -> {
                    Character status = productDocument.getStatus();
                    return '1' == status || 'T' == status;
                })
                .map(productDocument -> {
                    DiscountInfo discountInfo = productDiscountService.getDiscountInfo(Long.valueOf(productDocument.getId()));
                    return new ProductSearchDto(productDocument, discountInfo.getDiscountRate(), discountInfo.getDiscountedPrice());
                })
                .collect(Collectors.toList());

        // 필터링된 결과로 새로운 Page 객체 생성
        // 필터링 후 실제 데이터 개수로 페이지 정보 재구성
        Page<ProductSearchDto> returnResults = new PageImpl<>(
                dtoList,
                PageRequest.of(page, size),
                dtoList.size() // 필터링된 결과의 총 개수
        );

        return ResponseEntity.ok(returnResults);
    }
}