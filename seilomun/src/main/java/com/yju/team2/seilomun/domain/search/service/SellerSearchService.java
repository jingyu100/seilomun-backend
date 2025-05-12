package com.yju.team2.seilomun.domain.search.service;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.search.enums.ProductSortType;
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
    public Page<SellerDocument> searchSellers(String keyword, String category, Boolean deliveryOnly,
                                              String sortType, int page, int size) {

        // 1. 정렬 조건 생성
        Sort sort = createSort(sortType);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<SellerDocument> result;

        result = sellerSearchRepository.findByStoreNameContainingIgnoreCaseOrStoreDescriptionContainingIgnoreCase(
                keyword, keyword, pageRequest);

        return result;
    }

    // 가게 정보를 Elasticsearch에 인덱싱
    public void indexSellerDocument(SellerDocument sellerDocument) {
        sellerSearchRepository.save(sellerDocument);
    }

    // 가게 정보를 Elasticsearch에서 삭제
    public void deleteSellerDocument(String sellerId) {
        sellerSearchRepository.deleteById(sellerId);
    }

    // 정렬 조건 생성 메서드
    private Sort createSort(String sortType) {
        switch (sortType) {
            case "RATING_DESC":
                return Sort.by(Sort.Direction.DESC, "rating");
            case "RATING_ASC":
                return Sort.by(Sort.Direction.DESC, "rating");
            case "NEWEST":
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }
}