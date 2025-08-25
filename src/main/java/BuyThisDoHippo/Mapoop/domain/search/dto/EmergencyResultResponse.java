package BuyThisDoHippo.Mapoop.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyResultResponse {
    private int totalCount;
    private List<EmergencyResponse> toilets;
}
