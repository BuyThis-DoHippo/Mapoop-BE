package BuyThisDoHippo.Mapoop.domain.map.controller;

import BuyThisDoHippo.Mapoop.domain.map.dto.MapResultDto;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerFilterDto;
import BuyThisDoHippo.Mapoop.domain.map.service.MapService;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    @GetMapping("/markers")
    public CommonResponse<MapResultDto> getMarkers(
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean isGenderSeparated,
            @RequestParam(required = false) Boolean hasAccessibleToilet,
            @RequestParam(required = false) Boolean hasDiaperTable
    ) {
        log.debug("지도 마커 요청");
        ToiletType typed = null;
        if (type != null && !type.isBlank()) {
            try { typed = ToiletType.valueOf(type.toUpperCase()); }
            catch (IllegalArgumentException ignore) { /* 잘못된 값: 필터 미적용 */ }
        }

        MarkerFilterDto filter = MarkerFilterDto.builder()
                .minRating(minRating)
                .type(typed)
                .isAvailable(isAvailable)
                .isGenderSeparated(isGenderSeparated)
                .hasAccessibleToilet(hasAccessibleToilet)
                .hasDiaperTable(hasDiaperTable)
                .build();

        MapResultDto result = mapService.getMapResult(filter);
        return CommonResponse.onSuccess(result, "마커 데이터 조회 성공");
    }
}
