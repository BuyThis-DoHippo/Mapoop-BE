package BuyThisDoHippo.Mapoop.domain.search.controller;

import BuyThisDoHippo.Mapoop.domain.search.dto.SearchSuggestionDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchCriteriaDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchResultDto;
import BuyThisDoHippo.Mapoop.domain.search.service.SearchService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/results")
    public CommonResponse<SearchResultDto> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int pageSize,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        log.debug("검색 요청 - 쿼리: '{}', 페이지: {}, 크기: {}",keyword, page, pageSize);
        // keyword input 검증
        if (keyword == null || keyword.trim().isEmpty()) {
            SearchResultDto emptyResult = SearchResultDto.builder()
                    .totalCount(0L)
                    .toilets(Collections.emptyList())
                    .currentPage(page)
                    .totalPages(0)
                    .build();

            return CommonResponse.onSuccess(emptyResult, "검색어가 없습니다.");
        }

        SearchCriteriaDto criteria = SearchCriteriaDto.builder()
                .keyword(keyword)
                .page(page)
                .pageSize(pageSize)
                .build();

        // 있다면 search 호출
        SearchResultDto result = searchService.search(criteria, lat, lng);
        return CommonResponse.onSuccess(result, "검색 결과 조회 성공");
    }

    @GetMapping("/auto")
    public CommonResponse<Map<String, Object>> autoComplete(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "8") int limit
    ) {
        log.debug("자동완성 요청 - 쿼리: '{}'", keyword);

        List<SearchSuggestionDto> suggestions = searchService.getAutoCompleteSuggestions(keyword.trim());
        Map<String, Object> data = Map.of(
            "keyword", keyword,
            "totalCount", suggestions.size(),
            "limit", limit,
            "suggestions", suggestions
        );

        return CommonResponse.onSuccess(data, "자동완성 조회 성공");
    }
}
