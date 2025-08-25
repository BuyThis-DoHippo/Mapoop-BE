/**
 * 리뷰 작성/수정 요청 DTO
 * 클라이언트에서 서버로 보내는 리뷰 데이터
 */
package BuyThisDoHippo.Mapoop.domain.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor // JSON 역직렬화를 위한 기본 생성자 (Jackson이 JSON → 객체 변환할 때 필요)
public class ReviewRequest {
    
    /**
     * 별점 (1, 2, 3, 4, 5)
     * @NotNull: null 값 불허 (필수 입력)
     * @Min/Max: 최소/최대값 검증 (정수형 검증)
     */
    @NotNull(message = "별점은 필수입니다")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5 이하여야 합니다")
    private Integer rating;
    
    /**
     * 리뷰 제목 (선택사항)
     * @Size: 길이 제한 (1~100자)
     */
    @Size(max = 100, message = "제목은 100자 이하여야 합니다")
    private String title;
    
    /**
     * 리뷰 내용
     * @NotBlank: 공백/null/빈 문자열 불허 (문자열 필수 검증에 사용)
     */
    @NotBlank(message = "내용은 필수입니다")
    private String content;
    
    /**
     * 선택된 태그 ID 목록
     * 새로 추가되는 필드! (기존에 없던 태그 기능)
     * 예: [1, 3, 5] → "현재이용가능", "24시간", "비데있음"
     */
    private List<Long> tagIds;
    
    /**
     * 리뷰 이미지 URL 목록
     * 새로 추가되는 필드! (한 줄에 4개씩 표시할 이미지들)
     * 예: ["image1.jpg", "image2.jpg", "image3.jpg"]
     */
    private List<String> imageUrls;
}
