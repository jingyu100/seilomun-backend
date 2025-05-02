package com.yju.team2.seilomun.domain.search.repository;

import com.yju.team2.seilomun.domain.search.entity.SearchSuggestion;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SearchSuggestionRepository extends ElasticsearchRepository<SearchSuggestion, String> {

    SearchSuggestion findByKeyword(String keyword);

}