package BuyThisDoHippo.Mapoop.domain.search.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyResponse {
    private Long toiletId;
    private String type;
    private Double latitude;
    private Double longitude;
    private String name;
    private Double rating;
    private List<String> tags;
    private String address;
    private Integer distance;
    private Boolean isOpenNow;
    private Boolean isOpen24h;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String mainImageUrl;
}
