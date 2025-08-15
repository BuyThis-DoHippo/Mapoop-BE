package BuyThisDoHippo.Mapoop.domain.map.controller;

import BuyThisDoHippo.Mapoop.domain.map.dto.MapResultDto;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerFilterDto;
import BuyThisDoHippo.Mapoop.domain.map.service.MapService;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.GenderType;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
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

    /// TODO: 필터링 항목 추가 (검색과 동일하게, 상태 태그 추가)
    @GetMapping("/markers")
    public CommonResponse<MapResultDto> getMarkers(
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean isGenderSeparated,
            @RequestParam(required = false) Boolean hasAccessibleToilet,
            @RequestParam(required = false) Boolean hasDiaperTable,
            @RequestParam(required = false) Boolean isOpen24h,
            @RequestParam(required = false) Boolean hasIndoorToilet,
            @RequestParam(required = false) Boolean hasBidet,
            @RequestParam(required = false) Boolean providesSanitaryItems
    ) {
        log.debug("지도 마커 요청");

        ToiletType toiletType;
        if (type != null) {
            toiletType = ToiletType.fromString(type)
                    .orElseThrow(() -> new ApplicationException(CustomErrorCode.INVALID_TOILET_TYPE));
        } else {
            toiletType = null;
        }

        GenderType genderType;
        if(isGenderSeparated != null) {
            genderType = GenderType.fromString(isGenderSeparated ? "SEPARATE" : "UNISEX")
                    .orElseThrow(() -> new ApplicationException(CustomErrorCode.INVALID_GENDER_TYPE));
        } else {
            genderType = null;
        }

        MarkerFilterDto filter = MarkerFilterDto.builder()
                .minRating(minRating)
                .type(toiletType)
                .genderType(genderType)
                .isAvailable(isAvailable)
                .hasAccessibleToilet(hasAccessibleToilet)
                .hasDiaperTable(hasDiaperTable)
                .hasBidet(hasBidet)
                .providesSanitaryItems(providesSanitaryItems)
                .hasIndoorToilet(hasIndoorToilet)
                .isOpen24h(isOpen24h)
                .build();

        MapResultDto result = mapService.getMapResult(filter);
        return CommonResponse.onSuccess(result, "마커 데이터 조회 성공");
    }
}
