package com.yju.team2.seilomun.domain.product.service;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.enums.ProductFilterType;
import com.yju.team2.seilomun.domain.product.enums.ProductSortType;
import com.yju.team2.seilomun.domain.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // 기본 키워드 검색
//    public List<ProductDocument> searchByKeyword(String keyword) {
//        return productSearchRepository.findByNameContaining(keyword);
//    }

    // 키워드 + 필터링 + 정렬
    public List<ProductDocument> advancedSearch(String keyword, ProductFilterType filterType, ProductSortType sortType) {
        // 기본 검색 조건 생성
        Criteria criteria = new Criteria();

        // 키워드 검색
        if (keyword != null && !keyword.isEmpty()) {
            criteria = criteria.and(new Criteria("name").contains(keyword)
                    .or(new Criteria("description").contains(keyword)));
        }

        // 필터 적용
        LocalDateTime now = LocalDateTime.now();
        switch (filterType) {
            case RECENT:
                // 7일 내 등록 상품
                LocalDateTime sevenDaysAgo = now.minusDays(7);
                criteria = criteria.and(new Criteria("createdAt").greaterThanEqual(sevenDaysAgo));
                break;
            case EXPIRING_SOON:
                // 7일 내 유통기한 만료 상품
                LocalDateTime sevenDaysLater = now.plusDays(7);
                criteria = criteria.and(new Criteria("expiryDate").lessThanEqual(sevenDaysLater)
                        .and(new Criteria("expiryDate").greaterThanEqual(now)));
                break;
            case ALL:
            default:
                // 필터 없음
                break;
        }

        // 쿼리 생성
        CriteriaQuery query = new CriteriaQuery(criteria);

        // 정렬 적용
        switch (sortType) {
            case LATEST:
                query.addSort(Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
            case HIGHEST_RATING:
                query.addSort(Sort.by(Sort.Direction.DESC, "averageRating"));
                break;
            case LOWEST_RATING:
                query.addSort(Sort.by(Sort.Direction.ASC, "averageRating"));
                break;
            case HIGHEST_PRICE:
                query.addSort(Sort.by(Sort.Direction.DESC, "originalPrice"));
                break;
            case LOWEST_PRICE:
                query.addSort(Sort.by(Sort.Direction.ASC, "originalPrice"));
                break;
        }

        // 검색 실행
        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(query, ProductDocument.class, IndexCoordinates.of("products"));

        // 결과 변환
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    // 7일 내 등록된 상품 검색
//    public List<ProductDocument> searchRecentProducts(String keyword) {
//        return productSearchRepository.findByNameContainingAndCreatedWithinLastWeek(keyword);
//    }

    // 7일 내 유통기한이 만료되는 상품 검색
//    public List<ProductDocument> searchExpiringProducts(String keyword) {
//        return productSearchRepository.findByNameContainingAndExpiringWithinNextWeek(keyword);
//    }
}