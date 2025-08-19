package BuyThisDoHippo.Mapoop.domain.map.controller;

import BuyThisDoHippo.Mapoop.domain.map.dto.MapResultResponse;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerFilter;
import BuyThisDoHippo.Mapoop.domain.map.service.MapService;
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

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    @GetMapping("/markers")
    public CommonResponse<MapResultResponse> getMarkers(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) ToiletType type,
            @RequestParam(required = false) List<String> tags
    ) {
        log.debug("지도 마커 요청");

        List<String> raw = (tags == null ? List.<String>of()
                : tags.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .distinct()
                .toList());

        boolean requireAvailable = raw.stream()
                .anyMatch(t -> t.equalsIgnoreCase(TagConstants.VIRTUAL_AVAILABLE));

        List<String> normalizedTags = raw.stream()
                .filter(t -> !t.equalsIgnoreCase(TagConstants.VIRTUAL_AVAILABLE))
                .toList();

        MarkerFilter filter = MarkerFilter.builder()
                .lat(lat)
                .lng(lng)
                .minRating(minRating)
                .type(type)
                .tags(normalizedTags)
                .requireAvailable(requireAvailable)
                .build();

        MapResultResponse response = mapService.getMarkers(filter);
        return CommonResponse.onSuccess(response, "마커 데이터 조회 성공");
    }
}
