package BuyThisDoHippo.Mapoop.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteriaDto {
    private String keyword;
    private int page = 0;
    private int pageSize = 10;
//    private List<String> tags;
//    private Double minRating;
}
