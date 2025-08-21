/**
 * 리뷰 응답 DTO
 * 서버에서 클라이언트로 보내는 리뷰 데이터
 * 리뷰 상세 정보 + 태그 + 이미지 포함
 */
package BuyThisDoHippo.Mapoop.domain.review.dto;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder // 빌더 패턴: 선택적 매개변수가 많을 때 가독성 좋은 객체 생성
@Setter
public class ReviewResponse {
    
    // === 리뷰 기본 정보 ===
    
    /**
     * 리뷰 고유 ID
     */
    private Long reviewId;
    
    /**
     * 별점 (1, 2, 3, 4, 5)
     */
    private Integer rating;
    
    /**
     * 리뷰 제목
     */
    private String title;
    
    /**
     * 리뷰 내용
     */
    private String content;
    
    /**
     * 작성일시
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;
    
    // === 작성자 정보 ===
    
    /**
     * 작성자 ID
     */
    private Long userId;
    
    /**
     * 작성자 이름
     */
    private String userName;
    
    // === 화장실 정보 ===
    
    /**
     * 화장실 ID
     */
    private Long toiletId;
    
    /**
     * 화장실 이름
     */
    private String toiletName;
    
    // === 새로 추가되는 필드들 ===
    
    /**
     * 태그 목록 (새로 추가!)
     * 리뷰에 선택된 태그들 (예: "깨끗함", "24시간", "비데있음")
     */
    private List<TagResponse> tags;
    
    /**
     * 이미지 URL 목록 (새로 추가!)
     * 리뷰에 첨부된 이미지들 (한 줄에 4개씩 표시)
     */
    private List<String> imageUrls;
    
    /**
     * Review Entity → ReviewResponse DTO 변환 메서드
     * 
     * static 메서드인 이유:
     * - 유틸성 메서드 (인스턴스 없이 사용)
     * - 팩토리 패턴 (객체 생성 로직 캡슐화)
     * - 코드 재사용성 증가
     * 
     * @param review Review 엔티티
     * @return ReviewResponse DTO
     */
    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())  // ← nickname → name으로 수정
                .toiletId(review.getToilet().getId())
                .toiletName(review.getToilet().getName())
                // tags와 imageUrls는 Service 레이어에서 별도로 설정
                // → 성능상 이유로 지연 로딩하여 필요할 때만 조회
                .build();
    }
    
    /**
     * 태그와 이미지 정보를 추가로 설정하는 메서드
     * Service에서 호출하여 추가 데이터 설정
     */
    public static ReviewResponse withTagsAndImages(Review review, 
                                                   List<TagResponse> tags, 
                                                   List<String> imageUrls) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())    // ← nickname → name으로 수정
                .toiletId(review.getToilet().getId())
                .toiletName(review.getToilet().getName())
                .tags(tags)          // 추가 데이터
                .imageUrls(imageUrls) // 추가 데이터
                .build();
    }
}
