package com.yju.team2.seilomun.domain.search.repository;

import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.seller.entity.SellerDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerSearchRepository extends ElasticsearchRepository<SellerDocument, String> {

    // 이름과 설명으로 검색
    Page<SellerDocument> findByStoreNameContainingIgnoreCaseOrStoreDescriptionContainingIgnoreCase(
            String nameKeyword, String descriptionKeyword, Pageable pageable);

}