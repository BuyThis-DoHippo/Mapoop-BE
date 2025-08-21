package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.projection.ToiletWithDistance;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/* Search 응답을 위한 화장실 기본 정보 Dto */
public class ToiletInfo {
    private Long toiletId;
    private String name;
    private String type;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer floor;
    private Double rating;
    private Integer distance;    // m 단위
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
            .rating(toilet.getAvgRating())
            .tags(toilet.getToiletTags().stream()
                    .map(toiletTag -> toiletTag.getTag().getName()
                    ).toList())
            .isPartnership(toilet.getIsPartnership())
            .build();
    }

}
