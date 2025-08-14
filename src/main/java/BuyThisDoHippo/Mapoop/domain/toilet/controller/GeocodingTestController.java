package BuyThisDoHippo.Mapoop.domain.toilet.controller;

import BuyThisDoHippo.Mapoop.domain.toilet.service.GeocodingService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GeocodingTestController {
    private final GeocodingService geocodingService;
    @GetMapping("api/test/geocoding")
    public CommonResponse<Map<String, Object>> getLatLng(@RequestParam String address) {
        GeocodingService.GeoLocation geo = geocodingService.getGeoLocation(address);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestedAddress", address);
        body.put("formattedAddress", geo.getAddress());
        body.put("lat", geo.getLat());
        body.put("lon", geo.getLon());

        return CommonResponse.onSuccess(body, "좌표 변환 테스트 성공");
    }
}
