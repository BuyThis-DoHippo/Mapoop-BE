package BuyThisDoHippo.Mapoop.domain.map.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkerFilterDto {
    Double minRating;
    ToiletType type;                // 공공/민간
    Boolean isAvailable;
    Boolean isGenderSeparated;
    Boolean hasAccessibleToilet;
    Boolean hasDiaperTable;
}
