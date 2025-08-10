package BuyThisDoHippo.Mapoop.domain.search.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHomeDto {

    private Long totalCount;
    private List<ToiletInfo> toilets;
    private Double radiusKm;
    private Integer limit;

}
