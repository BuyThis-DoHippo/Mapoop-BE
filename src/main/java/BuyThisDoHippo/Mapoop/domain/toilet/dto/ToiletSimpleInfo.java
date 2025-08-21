package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/* 내가 등록한 화장실 응답을 위한 Dto */
public class ToiletSimpleInfo {
    private String name;
    private String type;
    private String address;
    private Integer floor;
    private LocalDate createdAt;
}
