package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToiletRegisterRequest {
    @NotBlank(message = "화장실 이름은 필수입니다")
    private String name;

    @NotBlank(message = "화장실 타입은 필수입니다 (\"STORE\" or \"PUBLIC\")")
    private String type;

    @NotBlank(message = "주소는 필수입니다")
    private String address;
    @NotNull(message = "층수는 필수입니다")
    private Integer floor;

    @NotNull(message = "24시간 운영 여부는 필수입니다")
    private Boolean isOpen24h;
    private LocalTime openTime;
    private LocalTime closeTime;

    @AssertTrue(message = "24시간 운영이 아닌 경우 운영시간은 필수입니다")
    public boolean isOperatingTimeValid() {
        if (Boolean.FALSE.equals(isOpen24h)) {
            return openTime != null && closeTime != null;
        }
        return true;
    }

    private String description;
    private String particulars;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private Boolean isPartnership = Boolean.FALSE;
}
