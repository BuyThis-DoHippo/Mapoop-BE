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
public class MarkerDto {
    private Long toiletId;
    private Double latitude;
    private Double longitude;
    private String name;
    private Double rating;
    private List<String> tags;
    private Boolean isOpenNow;
    private String address;
    private Integer floor;
    private Boolean isOpen24h;
    private LocalTime openTime;
    private LocalTime closeTime;
}
