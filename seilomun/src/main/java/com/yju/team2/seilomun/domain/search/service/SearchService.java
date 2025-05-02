package com.yju.team2.seilomun.domain.search.service;

import com.yju.team2.seilomun.domain.search.dto.AutoCompleteResponseDto;
import com.yju.team2.seilomun.domain.search.dto.PopularKeywordDto;
import com.yju.team2.seilomun.domain.search.dto.SearchHistoryDto;
import com.yju.team2.seilomun.domain.search.entity.SearchSuggestion;
import com.yju.team2.seilomun.domain.search.repository.SearchSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchSuggestionRepository searchSuggestionRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final String USER_SEARCH_HISTORY_KEY = "user:search:history:";
    private static final String GLOBAL_SEARCH_COUNTS_KEY = "global:search:counts";
    private static final long MAX_HISTORY_SIZE = 10; // 유저당 최대 검색 기록 수

    // 검색어 자동완성
    public AutoCompleteResponseDto getAutocompleteSuggestions(String prefix, int size) {

        // 접두어 검색 쿼리 생성
        Criteria criteria = new Criteria("keyword").startsWith(prefix);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(0, size));

        SearchHits<SearchSuggestion> searchHits = elasticsearchOperations.search(
                criteriaQuery,
                SearchSuggestion.class,
                IndexCoordinates.of("search_suggestions")
        );

        List<String> suggestions = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(SearchSuggestion::getKeyword)
                .collect(Collectors.toList());

        return AutoCompleteResponseDto.builder()
                .suggestions(suggestions)
                .build();
    }

    // 퍼지 검색(오타 교정) 자동완성 기능
    public AutoCompleteResponseDto getFuzzySuggestions(String term, int size) {

        // CriteriaQuery를 사용한 퍼지 검색 쿼리 생성
        // 퍼지 일치를 위해 "fuzzy" 연산자 사용

        // 검색어 길이에 따라 다르게 설정
        // fuzziness == 몇글자 틀려도 되는가
//        String fuzziness;
//        if (term.length() <= 4) {
//            fuzziness = "1";  // 짧은 검색어는 편집 거리 1
//        } else {
//            fuzziness = "2";  // 긴 검색어는 편집 거리 2
//        }

//        Criteria criteria = new Criteria("keyword").fuzzy(term, fuzziness);

        // 지금은 0~2은 0글자 / 3~5는 1글자 / 5글자 이상은 2글자 (AUTO)
        Criteria criteria = new Criteria("keyword").fuzzy(term);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(0, size));

        SearchHits<SearchSuggestion> searchHits = elasticsearchOperations.search(
                criteriaQuery,
                SearchSuggestion.class,
                IndexCoordinates.of("search_suggestions")
        );

        List<String> suggestions = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(SearchSuggestion::getKeyword)
                .collect(Collectors.toList());

        return AutoCompleteResponseDto.builder()
                .suggestions(suggestions)
                .build();
    }

    // 검색 기록 저장
    @Transactional
    public SearchHistoryDto saveSearchHistory(Long userId, Character userType, String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어가 비어있습니다");
        }

        String normalizedKeyword = keyword.trim();
        String userKey = USER_SEARCH_HISTORY_KEY + userType + ":" + userId;
        LocalDateTime now = LocalDateTime.now();

        // Redis Sorted Set에 검색 기록 저장 (점수 = 타임스탬프)
        double score = now.toEpochSecond(ZoneOffset.UTC) + 9;

        // 이미 존재하는 검색어라면 삭제 후 다시 추가 (최신 시간으로 업데이트)
        redisTemplate.opsForZSet().remove(userKey, normalizedKeyword);
        redisTemplate.opsForZSet().add(userKey, normalizedKeyword, score);

        // 최대 검색 기록 개수 유지
        if (redisTemplate.opsForZSet().size(userKey) > MAX_HISTORY_SIZE) {
            redisTemplate.opsForZSet().removeRange(userKey, 0, 0); // 가장 오래된 항목 제거
        }

        // 글로벌 검색어 카운트 증가 (Redis Sorted Set)
        redisTemplate.opsForZSet().incrementScore(GLOBAL_SEARCH_COUNTS_KEY, normalizedKeyword, 1);

        // 자동완성 인덱스 업데이트
        updateSearchSuggestion(normalizedKeyword);

        // 결과 DTO 생성
        return SearchHistoryDto.builder()
                .keyword(normalizedKeyword)
                .searchedAt(now)
                .build();
    }

    // 검색어 자동완성 데이터 업데이트
    private void updateSearchSuggestion(String keyword) {

        // 해당 키워드의 검색 빈도수 계산
        Double score = redisTemplate.opsForZSet().score(GLOBAL_SEARCH_COUNTS_KEY, keyword);
        Long count = score != null ? score.longValue() : 1L;

        // 기존 자동완성 데이터가 있는지 확인
        SearchSuggestion existingSuggestion = searchSuggestionRepository.findByKeyword(keyword);

        if (existingSuggestion != null) {
            // 기존 데이터 업데이트
            existingSuggestion.setWeight(count);
            searchSuggestionRepository.save(existingSuggestion);
        } else {
            // 새 데이터 생성
            SearchSuggestion newSuggestion = SearchSuggestion.from(keyword, count);
            searchSuggestionRepository.save(newSuggestion);
        }
    }

    // 사용자별 검색 기록 조회
    public List<SearchHistoryDto> getUserSearchHistory(Long userId, Character userType, int page, int size) {
        String userKey = USER_SEARCH_HISTORY_KEY + userType + ":" + userId;

        // Redis Sorted Set에서 역순으로 가져오기 (최신순)
        // 페이징 처리
        int start = page * size;
        int end = start + size - 1;

        Set<ZSetOperations.TypedTuple<Object>> results = redisTemplate.opsForZSet()
                .reverseRangeWithScores(userKey, start, end);

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<SearchHistoryDto> historyList = new ArrayList<>();

        for (ZSetOperations.TypedTuple<Object> tuple : results) {
            String keyword = (String) tuple.getValue();
            Double score = tuple.getScore();

            if (keyword != null && score != null) {
                // 스코어(타임스탬프)를 LocalDateTime으로 변환
                LocalDateTime searchedAt = LocalDateTime.ofEpochSecond(
                        score.longValue(), 0, ZoneOffset.UTC);

                historyList.add(SearchHistoryDto.builder()
                        .keyword(keyword)
                        .searchedAt(searchedAt)
                        .build());
            }
        }

        return historyList;
    }

    // 인기 검색어 조회
    public List<PopularKeywordDto> getPopularKeywords(int limit) {

        // Redis Sorted Set에서 상위 N개 검색어 가져오기 (점수 높은 순)
        Set<ZSetOperations.TypedTuple<Object>> results = redisTemplate.opsForZSet()
                .reverseRangeWithScores(GLOBAL_SEARCH_COUNTS_KEY, 0, limit - 1);

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<PopularKeywordDto> popularKeywords = new ArrayList<>();

        for (ZSetOperations.TypedTuple<Object> tuple : results) {
            String keyword = (String) tuple.getValue();
            Double score = tuple.getScore();

            if (keyword != null && score != null) {
                popularKeywords.add(PopularKeywordDto.builder()
                        .keyword(keyword)
                        .count(score.longValue())
                        .build());
            }
        }

        return popularKeywords;
    }

    // 사용자의 특정 검색어 삭제
    public boolean deleteSearchKeyword(Long userId, Character userType, String keyword) {
        String userKey = USER_SEARCH_HISTORY_KEY + userType + ":" + userId;
        Long removed = redisTemplate.opsForZSet().remove(userKey, keyword);
        return removed != null && removed > 0;
    }

    // 사용자의 모든 검색 기록 삭제
    public void clearSearchHistory(Long userId, Character userType) {
        String userKey = USER_SEARCH_HISTORY_KEY + userType + ":" + userId;
        redisTemplate.delete(userKey);
    }
}