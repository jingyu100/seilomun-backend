package com.yju.team2.seilomun.domain.product.repository;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    // 기본 키워드 검색
    List<ProductDocument> findByNameContaining(String keyword);

    // 설명에서도 검색
    List<ProductDocument> findByNameContainingOrDescriptionContaining(String nameKeyword, String descKeyword);

    // 7일 내 등록된 상품 검색
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}], \"filter\": [{\"range\": {\"createdAt\": {\"gte\": \"now-7d\"}}}]}}")
    List<ProductDocument> findByNameContainingAndCreatedWithinLastWeek(String keyword);

    // 7일 내 유통기한이 만료되는 상품 검색
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}], \"filter\": [{\"range\": {\"expiryDate\": {\"lte\": \"now+7d\", \"gte\": \"now\"}}}]}}")
    List<ProductDocument> findByNameContainingAndExpiringWithinNextWeek(String keyword);
}