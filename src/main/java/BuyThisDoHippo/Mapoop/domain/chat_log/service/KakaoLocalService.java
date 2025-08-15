package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoLocalService {

    @Value("${kakao.api.rest-key}")
    private String kakaoRestKey;

    // 매 요청 시 키를 확실히 반영하는 WebClient 생성
    private WebClient client() {
        if (kakaoRestKey == null || kakaoRestKey.isBlank()) {
            throw new IllegalStateException("Kakao REST API key is missing. Set 'kakao.api.rest-key'.");
        }
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + kakaoRestKey.trim())
                .build();
    }
    /**
     * 반경 내 화장실 검색 (기본 반경 500m)
     */
    public List<PlaceDto> searchToilets(double lat, double lng, int radiusMeters) {
        KakaoSearchResponse res = client().get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", "화장실")
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", radiusMeters)
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(KakaoSearchResponse.class)
                .block();

        if (res == null || res.documents == null) {
            return List.of();
        }

        return res.documents.stream()
                .map(doc -> new PlaceDto(
                        doc.place_name,
                        doc.road_address_name != null && !doc.road_address_name.isBlank()
                            ? doc.road_address_name : doc.address_name,
                        extractFloor(doc.place_name),
                        "도보 약 3분", // TODO: 거리 계산 로직 추가
                        "정보 없음" // TODO: 영업시간 API로 보강 가능
                ))
                .toList();
    }

    private String extractFloor(String name) {
        if (name == null) return "";
        var m = name.matches(".*(B\\d|\\d층).*") ? name.replaceAll(".*(B\\d|\\d층).*", "$1") : "";
        return m;
    }

    // DTO
    @Data
    public static class PlaceDto {
        private final String name;
        private final String roadAddress;
        private final String buildingFloor;
        private final String walkTime;
        private final String hours;
    }

    @Data
    public static class KakaoSearchResponse {
        private List<Document> documents;

        @Data
        public static class Document {
            private String place_name;
            private String road_address_name;
            private String address_name;
            private String phone;
            private double x; // 경도
            private double y; // 위도
        }
    }


}
