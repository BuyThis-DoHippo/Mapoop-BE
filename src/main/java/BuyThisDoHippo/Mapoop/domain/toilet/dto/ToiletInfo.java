package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer distance;
    private List<String> tags;

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
            .build();
    }

}
