package BuyThisDoHippo.Mapoop.domain.search.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import lombok.*;

import java.util.List;

@Getter
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
