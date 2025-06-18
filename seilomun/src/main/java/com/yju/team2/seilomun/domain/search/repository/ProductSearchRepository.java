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

    // 기존 메서드들에 상태 조건 추가

    // === 기본 검색 (판매중인 상품만) ===
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatusNot(
            String nameKeyword, String descriptionKeyword, Character excludeStatus, Pageable pageable);

    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatusNotAndCreatedAtAfter(
            String nameKeyword, String descriptionKeyword, Character excludeStatus, LocalDateTime date, Pageable pageable);

    // === 유통기한 임박 상품 (현재시간 < 유통기한 < 7일후, 판매중인 상품만) ===
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatusNotAndExpiryDateAfterAndExpiryDateBefore(
            String nameKeyword, String descriptionKeyword, Character excludeStatus,
            LocalDateTime expiryAfter, LocalDateTime expiryBefore, Pageable pageable);

    // === 카테고리 + 기본 검색 ===
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCategoryIdAndStatusNot(
            String nameKeyword, String descriptionKeyword, Long categoryId, Character excludeStatus, Pageable pageable);

    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCategoryIdAndStatusNotAndCreatedAtAfter(
            String nameKeyword, String descriptionKeyword, Long categoryId, Character excludeStatus, LocalDateTime date, Pageable pageable);

    // === 카테고리 + 유통기한 임박 ===
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCategoryIdAndStatusNotAndExpiryDateAfterAndExpiryDateBefore(
            String nameKeyword, String descriptionKeyword, Long categoryId, Character excludeStatus,
            LocalDateTime expiryAfter, LocalDateTime expiryBefore, Pageable pageable);

    // === 카테고리만 검색 ===
    Page<ProductDocument> findByCategoryIdAndStatusNot(Long categoryId, Character excludeStatus, Pageable pageable);

    Page<ProductDocument> findByCategoryIdAndStatusNotAndCreatedAtAfter(Long categoryId, Character excludeStatus, LocalDateTime date, Pageable pageable);

    Page<ProductDocument> findByCategoryIdAndStatusNotAndExpiryDateAfterAndExpiryDateBefore(
            Long categoryId, Character excludeStatus, LocalDateTime expiryAfter, LocalDateTime expiryBefore, Pageable pageable);

    // === 전체 상품 (상태 필터만) ===
    Page<ProductDocument> findByStatusNot(Character excludeStatus, Pageable pageable);

    Page<ProductDocument> findByStatusNotAndCreatedAtAfter(Character excludeStatus, LocalDateTime date, Pageable pageable);

    Page<ProductDocument> findByStatusNotAndExpiryDateAfterAndExpiryDateBefore(
            Character excludeStatus, LocalDateTime expiryAfter, LocalDateTime expiryBefore, Pageable pageable);
}