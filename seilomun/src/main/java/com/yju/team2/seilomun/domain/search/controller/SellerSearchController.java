package com.yju.team2.seilomun.domain.search.controller;

import com.yju.team2.seilomun.common.ApiResponseJson;
import com.yju.team2.seilomun.domain.search.service.SellerSearchService;
import com.yju.team2.seilomun.domain.seller.entity.SellerDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Slf4j
public class SellerSearchController {

    private final SellerSearchService sellerSearchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponseJson> searchSellers(@RequestParam(required = false, defaultValue = "") String keyword,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false, defaultValue = "false") Boolean deliveryOnly,
                                                         @RequestParam(required = false, defaultValue = "RATING_DESC") String sortType,
                                                         @RequestParam(required = false, defaultValue = "0") int page,
                                                         @RequestParam(required = false, defaultValue = "10") int size) {

        Page<SellerDocument> results = sellerSearchService.searchSellers(keyword, category, deliveryOnly, sortType, page, size);

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of(
                        "results", results.getContent(),
                        "totalElements", results.getTotalElements(),
                        "totalPages", results.getTotalPages(),
                        "currentPage", page,
                        "size", size,
                        "hasNext", results.hasNext()
                )
        ));
    }
}