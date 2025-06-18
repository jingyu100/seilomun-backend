package com.yju.team2.seilomun.domain.search.service;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.service.ProductDiscountService;
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

// 1. ProductSearchRepository에 상태 필터 추가된 메서드들
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductDiscountService productDiscountService;

    // 키워드 + 카테고리 + 필터링 + 정렬 + 페이징
    public Page<ProductDocument> searchProducts(String keyword, Long categoryId,
                                                ProductFilterType filterType,
                                                ProductSortType sortType, int page, int size) {

        // 1. 정렬 조건 생성
        Sort sort = createSort(sortType);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // 2. 검색어가 비어있는지 확인
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasCategory = categoryId != null;

        // 3. 현재 시간
        LocalDateTime now = LocalDateTime.now();

        // 4. 필터 타입과 카테고리에 따른 검색 수행
        Page<ProductDocument> result;

        switch (filterType) {
            case ALL:
                result = searchWithFilters(keyword, categoryId, hasKeyword, hasCategory, pageRequest, null, now);
                break;
            case RECENT:
                // 7일 이내 등록된 상품
                LocalDateTime sevenDaysAgo = now.minusDays(7);
                result = searchWithFilters(keyword, categoryId, hasKeyword, hasCategory, pageRequest, sevenDaysAgo, now);
                break;
            case EXPIRING_SOON:
                // 현재시간 이후 ~ 7일 후까지 유통기한 만료 예정인 상품 (판매중인 상품만)
                LocalDateTime sevenDaysLater = now.plusDays(7);
                result = searchWithExpiryFilters(keyword, categoryId, hasKeyword, hasCategory, pageRequest, now, sevenDaysLater);
                break;
            default:
                result = searchWithFilters(keyword, categoryId, hasKeyword, hasCategory, pageRequest, null, now);
        }

        return result;
    }

    // 일반 필터링 검색 (전체, 최신)
    private Page<ProductDocument> searchWithFilters(String keyword, Long categoryId,
                                                    boolean hasKeyword, boolean hasCategory,
                                                    PageRequest pageRequest, LocalDateTime createdAfter, LocalDateTime now) {

        // 판매중인 상품만 조회 (상태 'X', '0' 제외)
        Character excludeExpiredStatus = 'X';
        Character excludeStoppedStatus = '0';

        if (hasKeyword && hasCategory) {
            // 키워드 + 카테고리 검색
            if (createdAfter != null) {
                return productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCategoryIdAndStatusNotAndCreatedAtAfter(
                        keyword, keyword, categoryId, excludeExpiredStatus, createdAfter, pageRequest);
            } else {
                return productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCategoryIdAndStatusNot(
                        keyword, keyword, categoryId, excludeExpiredStatus, pageRequest);
            }
        } else if (hasKeyword) {
            // 키워드만 검색
            if (createdAfter != null) {
                return productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatusNotAndCreatedAtAfter(
                        keyword, keyword, excludeExpiredStatus, createdAfter, pageRequest);
            } else {
                return productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatusNot(
                        keyword, keyword, excludeExpiredStatus, pageRequest);
            }
        } else if (hasCategory) {
            // 카테고리만 검색
            if (createdAfter != null) {
                return productSearchRepository.findByCategoryIdAndStatusNotAndCreatedAtAfter(categoryId, excludeExpiredStatus, createdAfter, pageRequest);
            } else {
                return productSearchRepository.findByCategoryIdAndStatusNot(categoryId, excludeExpiredStatus, pageRequest);
            }
        } else {
            // 전체 검색 (판매중인 상품만)
            if (createdAfter != null) {
                return productSearchRepository.findByStatusNotAndCreatedAtAfter(excludeExpiredStatus, createdAfter, pageRequest);
            } else {
                return productSearchRepository.findByStatusNot(excludeExpiredStatus, pageRequest);
            }
        }
    }

    // 유통기한 임박 필터링 검색
    private Page<ProductDocument> searchWithExpiryFilters(String keyword, Long categoryId,
                                                          boolean hasKeyword, boolean hasCategory,
                                                          PageRequest pageRequest, LocalDateTime now, LocalDateTime expiryBefore) {

        // 판매중이면서 유통기한이 현재시간 이후이고 7일 이내에 만료되는 상품
        Character excludeExpiredStatus = 'X';

        if (hasKeyword && hasCategory) {
            // 키워드 + 카테고리 + 유통기한 임박
            return productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCategoryIdAndStatusNotAndExpiryDateAfterAndExpiryDateBefore(
                    keyword, keyword, categoryId, excludeExpiredStatus, now, expiryBefore, pageRequest);
        } else if (hasKeyword) {
            // 키워드 + 유통기한 임박
            return productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatusNotAndExpiryDateAfterAndExpiryDateBefore(
                    keyword, keyword, excludeExpiredStatus, now, expiryBefore, pageRequest);
        } else if (hasCategory) {
            // 카테고리 + 유통기한 임박
            return productSearchRepository.findByCategoryIdAndStatusNotAndExpiryDateAfterAndExpiryDateBefore(
                    categoryId, excludeExpiredStatus, now, expiryBefore, pageRequest);
        } else {
            // 전체 상품 중 유통기한 임박
            return productSearchRepository.findByStatusNotAndExpiryDateAfterAndExpiryDateBefore(
                    excludeExpiredStatus, now, expiryBefore, pageRequest);
        }
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