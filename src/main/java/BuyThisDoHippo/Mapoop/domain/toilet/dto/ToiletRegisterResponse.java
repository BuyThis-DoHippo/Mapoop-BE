package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToiletRegisterResponse {
    private Long id;
    private String name;
    private String type;
}
