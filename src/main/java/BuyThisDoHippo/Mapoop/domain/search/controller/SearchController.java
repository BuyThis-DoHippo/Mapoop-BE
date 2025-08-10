package BuyThisDoHippo.Mapoop.domain.search.controller;

import BuyThisDoHippo.Mapoop.domain.search.dto.SearchHomeDto;
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

    /**
     * 1. 가까운 화장실 목록 조회 (위도 경도 있다면)
     * 2. 리뷰 좋은 화장실 목록 조회 (없다면)
     */
    @GetMapping("/home")
    public CommonResponse<SearchHomeDto> homeSearch(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "3.0") Double radiusKm,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.debug("홈화면 화장실 목록 조회");

        SearchHomeDto result = searchService.searchNearby(lat, lng, radiusKm, limit);

        if(lat != null && lng != null) {
            return CommonResponse.onSuccess(result, "근처 화장실 조회 성공");
        }
        return CommonResponse.onSuccess(result, "리뷰 높은 순서대로 조회 성공");
    }
}
