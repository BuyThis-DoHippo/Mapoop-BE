package BuyThisDoHippo.Mapoop.domain.map.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkerFilter {
    private Double lat;   // 위도
    private Double lng;   // 경도
    private Double minRating;
    private ToiletType type;
    @Builder.Default
    private List<String> tags = List.of();

    private Boolean requireAvailable;  // true : 현재이용가능

}
