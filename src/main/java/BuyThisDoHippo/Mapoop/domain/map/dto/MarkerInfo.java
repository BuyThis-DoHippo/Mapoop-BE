package BuyThisDoHippo.Mapoop.domain.map.dto;

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
public class MarkerInfo {
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
