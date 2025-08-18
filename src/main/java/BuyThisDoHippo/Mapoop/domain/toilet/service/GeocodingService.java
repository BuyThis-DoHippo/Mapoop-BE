package BuyThisDoHippo.Mapoop.domain.toilet.service;

import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class GeocodingService {

    @Getter
    @Builder
    public static class GeoLocation {
        private Double lat;
        private Double lon;
        private String address; // Geocoding formatted address
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleGeocodingResponse {
        private String status;
        private List<GoogleGeocodingResult> results;

        @JsonProperty("error_message")
        private String errorMessage;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleGeocodingResult {
        @JsonProperty("formatted_address")
        private String formattedAddress;

        private GoogleGeometry geometry;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleGeometry {
        private GoogleLatLng location;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleLatLng {
        private Double lat;
        private Double lng;
    }

    @Value("${google.maps.api.key}")
    private String googleApiKey;

    private final WebClient webClient;

    public GeocodingService(@Qualifier("googleWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public GeoLocation getGeoLocation(String address) {
        if(address == null || address.trim().isEmpty()) {
            throw new ApplicationException(CustomErrorCode.GEOCODING_FAILED);
        }

        try {
            log.debug("주소 좌표 변환 요청 - 주소: {}", address.trim());
            GoogleGeocodingResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/geocode/json")
                            .queryParam("address", address.trim())
                            .queryParam("key", googleApiKey)
                            .queryParam("language", "ko")
                            .queryParam("region", "kr")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Google Maps API 오류");
                        return Mono.error(new ApplicationException(CustomErrorCode.GEOCODING_FAILED));
                    })
                    .bodyToMono(GoogleGeocodingResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            return parseGeocodingResponse(response, address.trim());
            
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.debug("주소 좌표 변환 요청 실패 - {}", e.getMessage());
            throw new ApplicationException(CustomErrorCode.GEOCODING_FAILED);
        }
    }

    private GeoLocation parseGeocodingResponse(GoogleGeocodingResponse response, String address) {
        if(response == null) {
            throw new ApplicationException(CustomErrorCode.GEOCODING_FAILED);
        }

        if (!"OK".equals(response.getStatus())) {
            log.warn("Google Maps API 오류 - status: {}, 에러메시지: {}, 주소: {}",
                    response.getStatus(), response.getErrorMessage(), address);
            throw new ApplicationException(CustomErrorCode.GEOCODING_FAILED);
        }

        if (response.getResults() == null || response.getResults().isEmpty()) {
            log.warn("주소에 대한 결과를 찾을 수 없음 - 주소: {}", address);
            throw new ApplicationException(CustomErrorCode.GEOCODING_FAILED);
        }

        GoogleGeocodingResult result = response.getResults().getFirst();
        if (result.getGeometry() == null || result.getGeometry().getLocation() == null) {
            log.warn("geometry 없음 - 주소: {}", address);
            throw new ApplicationException(CustomErrorCode.GEOCODING_FAILED);
        }

        GoogleLatLng location = result.getGeometry().getLocation();
        log.debug("좌표 변환 성공 - 주소: {} → 좌표: ({}, {})", address, location.getLat(), location.getLng());

        return GeoLocation.builder()
                .lat(location.getLat())
                .lon(location.getLng())
                .address(result.getFormattedAddress())
                .build();
    }
}
