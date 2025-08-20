package BuyThisDoHippo.Mapoop.domain.search.dto;

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
public class SearchFilter {
    private String keyword;
    private Double lat;   // 위도
    private Double lng;   // 경도
    private Double minRating;
    private ToiletType type;
    private List<String> tags = List.of();

    private Boolean requireAvailable;  // true : 현재이용가능

    public boolean hasLocation() { return lat != null && lng != null; }
}
