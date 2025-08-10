package BuyThisDoHippo.Mapoop.domain.search.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchFilterDto {
    private String keyword;
    private int page = 0;
    private int pageSize = 10;

    // 필터링 필드
    Double minRating;
    ToiletType type;                // 공공/민간
    Boolean isAvailable;            // 현재 이용 가능
    Boolean isGenderSeparated;      // 남녀 분리
    Boolean isOpen24h;              // 24시간
    Boolean hasIndoorToilet;        //가게 안 화장실
    Boolean hasBidet;                // 비데 설치 여부
    Boolean hasAccessibleToilet;    // 장애인 화장실
    Boolean hasDiaperTable;      // 기저귀 교환대
    Boolean providesSanitaryItems;       // 위생용품 제공 여부
}
