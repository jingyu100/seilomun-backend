package com.yju.team2.seilomun.domain.search.repository;

import com.yju.team2.seilomun.domain.seller.entity.SellerDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerSearchRepository extends ElasticsearchRepository<SellerDocument, String> {

}