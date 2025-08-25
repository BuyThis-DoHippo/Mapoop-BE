package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToiletDetailResponse {
    private Long id;
    private String name;
    private String type;

    private Location location;
    private Rating rating;
    private Hours hours;

    private boolean isPartnership;
    private String description;
    private String particulars;

    private List<String> images; // 이미지 URL 목록
    private List<String> tags;   // 태그 이름 목록

    // 위치
    @Getter
    @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class Location {
        private Double latitude;
        private Double longitude;
        private String address;
        private Integer floor;
    }

    // 평점
    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class Rating {
        private Double avgRating;
        private Integer totalReviews;
    }

    // 운영시간
    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class Hours {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        private LocalTime openTime;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        private LocalTime closeTime;
        private boolean isOpen24h;
        private boolean isOpenNow; // 서버가 계산해서 내려줌
    }
}
