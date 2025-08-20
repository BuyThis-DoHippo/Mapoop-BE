package BuyThisDoHippo.Mapoop.domain.user.dto;


import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletSimpleInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyToiletResponse {
    private int totalCount;
    private List<ToiletSimpleInfo> toilets;
}
