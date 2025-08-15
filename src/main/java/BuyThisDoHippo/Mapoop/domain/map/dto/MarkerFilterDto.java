package BuyThisDoHippo.Mapoop.domain.map.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.GenderType;
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
    ToiletType type;
    GenderType genderType;
    Boolean isAvailable;
    Boolean hasAccessibleToilet;
    Boolean hasDiaperTable;
    Boolean isOpen24h;              // 24시간
    Boolean hasIndoorToilet;        //가게 안 화장실
    Boolean hasBidet;                // 비데 설치 여부
    Boolean providesSanitaryItems;       // 위생용품 제공 여부
}
