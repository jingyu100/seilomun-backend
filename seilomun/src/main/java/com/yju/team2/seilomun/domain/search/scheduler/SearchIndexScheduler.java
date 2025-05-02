package com.yju.team2.seilomun.domain.search.scheduler;

import com.yju.team2.seilomun.domain.search.entity.SearchSuggestion;
import com.yju.team2.seilomun.domain.search.repository.SearchSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchSuggestionRepository searchSuggestionRepository;

    private static final String GLOBAL_SEARCH_COUNTS_KEY = "global:search:counts";
    private static final String USER_SEARCH_HISTORY_KEY = "user:search:history:";
    private static final long SIX_MONTHS_IN_SECONDS = 15552000L; // 6개월(초 단위)

    // 매일 새벽 3시에 검색어 자동완성 인덱스 업데이트
    // Redis의 인기 검색어를 기반으로 Elasticsearch 자동완성 인덱스 갱신
    @Scheduled(cron = "0 0 3 * * ?")
    public void updateSearchSuggestionIndex() {

        log.info("검색어 자동완성 인덱스 업데이트 작업 시작");

        try {
            // Redis에서 상위 1000개의 인기 검색어 가져오기
            Set<ZSetOperations.TypedTuple<Object>> popularKeywords =
                    redisTemplate.opsForZSet().reverseRangeWithScores(GLOBAL_SEARCH_COUNTS_KEY, 0, 999);

            if (popularKeywords == null || popularKeywords.isEmpty()) {
                log.info("인기 검색어가 없습니다. 업데이트를 건너뜁니다.");
                return;
            }

            int updated = 0;
            int created = 0;

            // 인기 검색어 기반으로 자동완성 인덱스 업데이트
            for (ZSetOperations.TypedTuple<Object> tuple : popularKeywords) {
                String keyword = (String) tuple.getValue();
                Double score = tuple.getScore();

                if (keyword == null || score == null) continue;

                Long count = score.longValue();

                // 기존 데이터가 있는지 확인
                SearchSuggestion existingSuggestion = searchSuggestionRepository.findByKeyword(keyword);

                if (existingSuggestion != null) {
                    // 기존 데이터 업데이트
                    existingSuggestion.setWeight(count);
                    searchSuggestionRepository.save(existingSuggestion);
                    updated++;
                } else {
                    // 새 데이터 생성
                    SearchSuggestion newSuggestion = SearchSuggestion.from(keyword, count);
                    searchSuggestionRepository.save(newSuggestion);
                    created++;
                }
            }

            log.info("검색어 자동완성 인덱스 업데이트 완료 - 업데이트: {}, 신규 생성: {}", updated, created);
        } catch (Exception e) {
            log.error("검색어 자동완성 인덱스 업데이트 중 오류 발생", e);
        }
    }

    // 매주 일요일 새벽 4시에 오래된 검색 기록 정리
    // 6개월 이상 된 Redis 검색 기록 삭제
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void cleanupOldSearchHistory() {
        log.info("오래된 검색 기록 정리 작업 시작");

        try {
            long cutoffTime = LocalDateTime.now().minusMonths(6).toEpochSecond(ZoneOffset.UTC);

            // Redis에서 모든 사용자의 검색 기록 키 패턴 검색
            Set<String> userHistoryKeys = redisTemplate.keys(USER_SEARCH_HISTORY_KEY + "*");

            if (userHistoryKeys == null || userHistoryKeys.isEmpty()) {
                log.info("정리할 검색 기록이 없습니다.");
                return;
            }

            int totalRemoved = 0;

            for (String userKey : userHistoryKeys) {
                // cutoffTime보다 오래된 검색 기록 삭제 (점수 기준)
                Long removed = redisTemplate.opsForZSet().removeRangeByScore(userKey, 0, cutoffTime);
                if (removed != null && removed > 0) {
                    totalRemoved += removed;
                    log.debug("키 '{}' 에서 오래된 검색 기록 {} 건 삭제", userKey, removed);
                }
            }

            log.info("오래된 검색 기록 정리 완료: 총 {} 건 삭제됨", totalRemoved);
        } catch (Exception e) {
            log.error("오래된 검색 기록 정리 중 오류 발생", e);
        }
    }
}