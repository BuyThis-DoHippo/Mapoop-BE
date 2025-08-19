package BuyThisDoHippo.Mapoop.domain.search.controller;

import BuyThisDoHippo.Mapoop.domain.search.dto.*;
import BuyThisDoHippo.Mapoop.domain.search.service.SearchService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.common.TagConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/results")
    public CommonResponse<SearchResultResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) ToiletType type,
            @RequestParam(required = false) List<String> tags
    ) {
        log.debug("검색 요청 - 쿼리: '{}'",keyword);
        List<String> raw = (tags == null ? List.<String>of() :
                tags.stream()
                        .flatMap(s -> Arrays.stream(s.split(",")))
                        .map(String::trim)
                        .filter(t -> !t.isBlank())
                        .distinct()
                        .toList());

        boolean requireAvailable = raw.stream()
                .anyMatch(t -> t.equalsIgnoreCase(TagConstants.VIRTUAL_AVAILABLE));

        // 현재이용가능 태그는 제거
        List<String> normalizedTags = raw.stream()
                .filter(t -> !t.equalsIgnoreCase(TagConstants.VIRTUAL_AVAILABLE))
                .toList();

        SearchFilter filter = SearchFilter.builder()
                .keyword(keyword)
                .lat(lat)
                .lng(lng)
                .minRating(minRating)
                .type(type)
                .tags(normalizedTags)
                .requireAvailable(requireAvailable)
                .build();

        SearchResultResponse response = searchService.search(filter);
        return CommonResponse.onSuccess(response, "검색 결과 조회 성공");
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
    public CommonResponse<SearchResultResponse> homeSearch(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "16") Integer limit
    ) {
        log.debug("홈화면 화장실 목록 조회");

        SearchResultResponse response = searchService.searchNearby(lat, lng, limit);

        if(lat != null && lng != null) {
            return CommonResponse.onSuccess(response, "근처 화장실 조회 성공");
        }
        return CommonResponse.onSuccess(response, "리뷰 높은 순서대로 조회 성공");
    }

    @GetMapping("/emergency")
    public CommonResponse<SearchResultResponse> emergencySearch(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "3.0") Double radiusKm
    ) {
        log.debug("긴급 찾기 조회");

        SearchResultResponse result = searchService.searchNearby(lat, lng, 5);
        return CommonResponse.onSuccess(result, "긴급 화장실 조회 성공");
    }

}
