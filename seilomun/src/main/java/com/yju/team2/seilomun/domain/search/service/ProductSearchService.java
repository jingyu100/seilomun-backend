package com.yju.team2.seilomun.domain.search.service;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.search.enums.ProductFilterType;
import com.yju.team2.seilomun.domain.search.enums.ProductSortType;
import com.yju.team2.seilomun.domain.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // 키워드 + 필터링 + 정렬 + 페이징
    public Page<ProductDocument> searchProducts(String keyword, ProductFilterType filterType,
                                                ProductSortType sortType, int page, int size) {
        // 기본 검색 조건 생성
        Criteria criteria = new Criteria();

        // 키워드 검색
        if (keyword != null && !keyword.isEmpty()) {
            criteria = criteria.and(new Criteria("name").contains(keyword).boost(2.0f)
                    .or(new Criteria("description").contains(keyword)).boost(1.0f));
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

        // 페이징 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 쿼리 생성
        CriteriaQuery query = new CriteriaQuery(criteria);

        // 페이징 적용
        query.setPageable(pageable);

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
        List<ProductDocument> productDocuments = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // Page 객체로 변환하여 반환
        return new PageImpl<>(productDocuments, pageable, searchHits.getTotalHits());
    }

    // 상품 정보를 Elasticsearch에 인덱싱
    public void indexProductDocument(ProductDocument productDocument) {
        productSearchRepository.save(productDocument);
    }

    // 상품 정보를 Elasticsearch에서 삭제
    public void deleteProductDocument(String productId) {
        productSearchRepository.deleteById(productId);
    }

}