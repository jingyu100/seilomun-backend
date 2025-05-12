package com.yju.team2.seilomun.domain.search.service;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.search.enums.ProductFilterType;
import com.yju.team2.seilomun.domain.search.enums.ProductSortType;
import com.yju.team2.seilomun.domain.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // 키워드 + 필터링 + 정렬 + 페이징
    public Page<ProductDocument> searchProducts(String keyword, ProductFilterType filterType,
                                                ProductSortType sortType, int page, int size) {
        // 1. 정렬 조건 생성
        Sort sort = createSort(sortType);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // 2. 필터 타입에 따른 검색 수행
        Page<ProductDocument> result;

        switch (filterType) {
            case ALL:
                result = productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        keyword, keyword, pageRequest);
                break;
            case RECENT:
                // 7일 이내 등록된 상품
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                result = productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCreatedAtAfter(
                        keyword, keyword, sevenDaysAgo, pageRequest);
                break;
            case EXPIRING_SOON:
                // 7일 이내 유통기한 만료 상품
                LocalDateTime sevenDaysLater = LocalDateTime.now().plusDays(7);
                result = productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndExpiryDateBefore(
                        keyword, keyword, sevenDaysLater, pageRequest);
                break;
            default:
                result = productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        keyword, keyword, pageRequest);
        }

        return result;
    }

    // 상품 정보를 Elasticsearch에 인덱싱
    public void indexProductDocument(ProductDocument productDocument) {
        productSearchRepository.save(productDocument);
    }

    // 상품 정보를 Elasticsearch에서 삭제
    public void deleteProductDocument(String productId) {
        productSearchRepository.deleteById(productId);
    }

    // 정렬 조건 생성 메서드
    private Sort createSort(ProductSortType sortType) {
        switch (sortType) {
            case LATEST:
                return Sort.by(Sort.Direction.DESC, "createdAt");
            case HIGHEST_RATING:
                return Sort.by(Sort.Direction.DESC, "averageRating");
            case LOWEST_RATING:
                return Sort.by(Sort.Direction.ASC, "averageRating");
            case HIGHEST_PRICE:
                return Sort.by(Sort.Direction.DESC, "originalPrice");
            case LOWEST_PRICE:
                return Sort.by(Sort.Direction.ASC, "originalPrice");
            case EXPIRING:
                return Sort.by(Sort.Direction.ASC, "expiryDate");
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

}