package com.yju.team2.seilomun.domain.search.repository;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    // 이름과 설명으로 검색
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String nameKeyword, String descriptionKeyword, Pageable pageable);

    // 새 상품 검색 ( ex : 등록 7일 이내 )
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCreatedAtAfter(
            String nameKeyword, String descriptionKeyword, LocalDateTime date, Pageable pageable);

    // 임박 상품 검색 ( ex : 만료 7일 전 )
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndExpiryDateBefore(
            String nameKeyword, String descriptionKeyword, LocalDateTime date, Pageable pageable);
}