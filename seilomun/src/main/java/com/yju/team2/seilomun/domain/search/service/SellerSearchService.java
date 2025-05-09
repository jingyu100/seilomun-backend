package com.yju.team2.seilomun.domain.search.service;

import com.yju.team2.seilomun.domain.search.repository.SellerSearchRepository;
import com.yju.team2.seilomun.domain.seller.entity.SellerDocument;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerSearchService {

    private final SellerSearchRepository sellerSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // 가게 검색 기능
    public Page<SellerDocument> searchSellers(String keyword,
                                              String category,
                                              Boolean deliveryOnly,
                                              String sortType,
                                              int page,
                                              int size) {

        // 기본 검색 조건 생성
        Criteria criteria = new Criteria();

        // 1. 키워드 검색 (가게명, 설명, 주소에서 검색)
        // 키워드 검색 부분 수정
        if (keyword != null && !keyword.isEmpty()) {
            criteria = criteria.and(new Criteria("storeName").contains(keyword).boost(3.0f)
                    .or(new Criteria("storeDescription").contains(keyword)).boost(2.0f)
                    .or(new Criteria("addressDetail").contains(keyword)).boost(1.0f));
        }

        // 2. 카테고리 필터링
//        if (category != null && !category.isEmpty()) {
//            criteria = criteria.and(new Criteria("categoryName").is(category));
//        }

        // 3. 배달 가능 여부 필터링
//        if (Boolean.TRUE.equals(deliveryOnly)) {
//            criteria = criteria.and(new Criteria("deliveryAvailable").is("Y"));
//        }

        // 4. 영업 중인 가게만 표시 (상태가 정상인 가게)
//        criteria = criteria.and(new Criteria("status").is("1"));

        // 페이징 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 쿼리 생성
        CriteriaQuery query = new CriteriaQuery(criteria);

        // 페이징 적용
        query.setPageable(pageable);

        // 정렬 적용
        if (sortType != null) {
            switch (sortType) {
                case "RATING_DESC":
                    query.addSort(Sort.by(Sort.Direction.DESC, "rating"));
                    break;
                case "RATING_ASC":
                    query.addSort(Sort.by(Sort.Direction.ASC, "rating"));
                    break;
                case "NEWEST":
                default:
                    query.addSort(Sort.by(Sort.Direction.DESC, "createdAt"));
                    break;
            }
        } else {
            // 기본 정렬은 평점 높은 순
            query.addSort(Sort.by(Sort.Direction.DESC, "rating"));
        }

        // 검색 실행
        SearchHits<SellerDocument> searchHits =
                elasticsearchOperations.search(query, SellerDocument.class, IndexCoordinates.of("sellers"));

        // 결과 변환
        List<SellerDocument> sellerDocuments = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        log.info("검색 키워드: {}", keyword);
        log.info("생성된 쿼리: {}", query);
        log.info("검색 결과 건수: {}", searchHits.getTotalHits());

        // Page 객체로 변환하여 반환
        return new PageImpl<>(sellerDocuments, pageable, searchHits.getTotalHits());
    }

    // 가게 정보를 Elasticsearch에 인덱싱
    public void indexSellerDocument(SellerDocument sellerDocument) {
        sellerSearchRepository.save(sellerDocument);
    }

    // 가게 정보를 Elasticsearch에서 삭제
    public void deleteSellerDocument(String sellerId) {
        sellerSearchRepository.deleteById(sellerId);
    }
}