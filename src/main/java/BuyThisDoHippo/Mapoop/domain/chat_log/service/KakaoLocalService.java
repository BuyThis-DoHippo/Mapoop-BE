package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "app.kakao", name = "enabled", havingValue = "true")
@Slf4j
public class KakaoLocalService {

    private final WebClient kakaoWebClient;

    public KakaoLocalService(@Value("${kakao.api.rest-key}") String kakaoRestKey) {
        this.kakaoWebClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + kakaoRestKey) // 주입 완료된 키 사용
                .build();
        log.info("KakaoLocal WebClient ready. (key length: {})",
                kakaoRestKey != null ? kakaoRestKey.length() : 0);
    }

    public List<PlaceDto> searchToilets(double lat, double lng, int radiusMeters) {
        KakaoSearchResponse res = kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", "화장실")
                        .queryParam("y", lat)   // Kakao: y=위도, x=경도
                        .queryParam("x", lng)
                        .queryParam("radius", radiusMeters) // 최대 20000
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(KakaoSearchResponse.class)
                .block();

        if (res == null || res.documents == null) return List.of();

        return res.documents.stream()
                .map(doc -> new PlaceDto(
                        doc.place_name,
                        (doc.road_address_name != null && !doc.road_address_name.isBlank())
                                ? doc.road_address_name : doc.address_name,
                        extractFloor(doc.place_name),
                        "도보 약 3분",
                        "정보 없음"
                ))
                .toList();
    }

    private String extractFloor(String name) {
        if (name == null) return "";
        return name.matches(".*(B\\d|\\d층).*") ? name.replaceAll(".*(B\\d|\\d층).*", "$1") : "";
    }

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

