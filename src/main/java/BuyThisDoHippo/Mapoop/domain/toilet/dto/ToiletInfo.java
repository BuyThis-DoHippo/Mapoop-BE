package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.projection.ToiletWithDistance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToiletInfo {
    private Long toiletId;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer floor;
    private Double rating;
    private Double distance;    // km 단위
    private List<String> tags;
    private Boolean isPartnership;

    public static ToiletInfo from(Toilet toilet) {
        // distance 빼고 반환
        return ToiletInfo.builder()
            .toiletId(toilet.getId())
            .name(toilet.getName())
            .latitude(toilet.getLatitude())
            .longitude(toilet.getLongitude())
            .address(toilet.getAddress())
            .floor(toilet.getFloor())
            .rating(toilet.getAvgRating())
            .tags(toilet.getTags().stream()
                    .map(toiletTag -> toiletTag.getTag().getName()
                    ).toList())
            .isPartnership(toilet.getIsPartnership())
            .build();
    }

    public static ToiletInfo fromProjection(ToiletWithDistance projection) {
        return ToiletInfo.builder()
                .toiletId(projection.getId())
                .name(projection.getName())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .address(projection.getAddress())
                .floor(projection.getFloor())
                .rating(projection.getAvgRating())
                .distance(projection.getDistance())
                .tags(new ArrayList<>()) // 나중에 추가
                .isPartnership(projection.getIsPartnership())
                .build();

    }

}
