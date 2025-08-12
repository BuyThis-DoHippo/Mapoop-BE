package BuyThisDoHippo.Mapoop.domain.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapResultDto {
    private Long totalCount;
    private List<MarkerDto> markers;
}
