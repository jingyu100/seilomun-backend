package com.yju.team2.seilomun.domain.search.controller;

import com.yju.team2.seilomun.domain.auth.JwtUserDetails;
import com.yju.team2.seilomun.domain.search.dto.AutoCompleteResponseDto;
import com.yju.team2.seilomun.domain.search.dto.PopularKeywordDto;
import com.yju.team2.seilomun.domain.search.dto.SearchHistoryDto;
import com.yju.team2.seilomun.domain.search.service.SearchService;
import com.yju.team2.seilomun.common.ApiResponseJson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    // 검색어 자동완성
    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponseJson> getAutocompleteSuggestions(
            @RequestParam String prefix,
            @RequestParam(required = false, defaultValue = "5") int size) {

        AutoCompleteResponseDto suggestions = searchService.getAutocompleteSuggestions(prefix, size);

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of("suggestions", suggestions.getSuggestions())
        ));
    }

    // 퍼지 검색(오타 교정) 자동완성
    @GetMapping("/fuzzy")
    public ResponseEntity<ApiResponseJson> getFuzzySuggestions(
            @RequestParam String term,
            @RequestParam(required = false, defaultValue = "5") int size) {

        AutoCompleteResponseDto suggestions = searchService.getFuzzySuggestions(term, size);

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of("suggestions", suggestions.getSuggestions())
        ));
    }

    // 검색 기록 저장
    @PostMapping("/history")
    public ResponseEntity<ApiResponseJson> saveSearchHistory(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam String keyword) {

        Long userId = userDetails.getId();
        Character userType = userDetails.getUserType().equals("CUSTOMER") ? 'C' : 'S';

        SearchHistoryDto history = searchService.saveSearchHistory(userId, userType, keyword);

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of(
                        "history", history,
                        "message", "검색 기록이 저장되었습니다."
                )
        ));
    }

    // 사용자별 검색 기록 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponseJson> getUserSearchHistory(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        Long userId = userDetails.getId();
        Character userType = userDetails.getUserType().equals("CUSTOMER") ? 'C' : 'S';

        List<SearchHistoryDto> histories = searchService.getUserSearchHistory(userId, userType, page, size);

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of(
                        "histories", histories,
                        "currentPage", page,
                        "size", size
                )
        ));
    }

    // 사용자별 검색 기록 삭제
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponseJson> deleteSearchHistory(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam String keyword) {

        Long userId = userDetails.getId();
        Character userType = userDetails.getUserType().equals("CUSTOMER") ? 'C' : 'S';

        boolean deleted = searchService.deleteSearchKeyword(userId, userType, keyword);

        if (deleted) {
            return ResponseEntity.ok(new ApiResponseJson(
                    HttpStatus.OK,
                    Map.of("message", "검색 기록이 삭제되었습니다.")
            ));
        } else {
            return ResponseEntity.ok(new ApiResponseJson(
                    HttpStatus.OK,
                    Map.of("message", "삭제할 검색 기록이 없습니다.")
            ));
        }
    }

    // 사용자별 검색 기록 전체 삭제
    @DeleteMapping("/history/all")
    public ResponseEntity<ApiResponseJson> clearSearchHistory(
            @AuthenticationPrincipal JwtUserDetails userDetails) {

        Long userId = userDetails.getId();
        Character userType = userDetails.getUserType().equals("CUSTOMER") ? 'C' : 'S';

        searchService.clearSearchHistory(userId, userType);

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of("message", "모든 검색 기록이 삭제되었습니다.")
        ));
    }

    // 인기 검색어 조회
    @GetMapping("/popular")
    public ResponseEntity<ApiResponseJson> getPopularKeywords(
            @RequestParam(required = false, defaultValue = "10") int limit) {

        List<PopularKeywordDto> popularKeywords = searchService.getPopularKeywords(limit);

        return ResponseEntity.ok(new ApiResponseJson(
                HttpStatus.OK,
                Map.of("popularKeywords", popularKeywords)
        ));
    }
}