package BuyThisDoHippo.Mapoop.domain.chat_log.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 챗봇 질문 요청 DTO
 * - question: 사용자의 자연어 질문 (필수)
 * - lat/lng/radius: 위치 기반 검색 옵션 (선택)
 * - minRating/accessibleOnly/openNow/limit: 필터 옵션 (선택)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatAskRequest {

    /** 사용자 질문 (필수) */
    @NotBlank(message = "질문을 입력해주세요")
    @Size(max = 500, message = "질문은 500자 이내로 입력해주세요")
    private String question;

    /** 사용자 위도 (WGS84) */
    @DecimalMin(value = "-90.0", message = "위도 범위는 -90~90 입니다")
    @DecimalMax(value = "90.0", message = "위도 범위는 -90~90 입니다")
    private Double lat;   // 예: 37.557

    /** 사용자 경도 (WGS84) */
    @DecimalMin(value = "-180.0", message = "경도 범위는 -180~180 입니다")
    @DecimalMax(value = "180.0", message = "경도 범위는 -180~180 입니다")
    private Double lng;   // 예: 126.925

    /** 검색 반경 (미터) */
    @Min(value = 50, message = "반경은 최소 50m 이상이어야 합니다")
    @Max(value = 5000, message = "반경은 최대 5000m 까지 허용됩니다")
    private Integer radius; // 기본 500 (서비스 레이어에서 보정 권장)

    /** 최소 별점 (0.0 ~ 5.0) */
    @DecimalMin(value = "0.0", message = "별점은 0.0 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다")
    private Double minRating; // 예: 3.5

    /** 장애인 화장실만 */
    private Boolean accessibleOnly; // true 이면 장애인 화장실 필터

    /** 현재 영업중(운영시간 내)만 */
    private Boolean openNow; // true 이면 현재 이용 가능 필터

    /** 최대 결과 개수 (선택, 웹/챗봇 용도) */
    @Min(value = 1, message = "최소 1개 이상 요청해야 합니다")
    @Max(value = 50, message = "최대 50개까지 요청할 수 있습니다")
    private Integer limit; // 기본 5~10 등 서비스에서 보정
}
