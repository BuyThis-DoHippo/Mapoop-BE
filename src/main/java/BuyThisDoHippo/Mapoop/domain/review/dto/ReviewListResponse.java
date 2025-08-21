/**
 * 리뷰 목록 응답 DTO
 * 페이징 정보와 함께 리뷰 목록을 반환
 * 화장실 상세 페이지나 사용자 리뷰 목록에서 사용
 */
package BuyThisDoHippo.Mapoop.domain.review.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class ReviewListResponse {
    
    /**
     * 리뷰 목록
     * 각 리뷰에는 태그와 이미지 정보도 포함
     */
    private List<ReviewResponse> reviews;
    
    // === 페이징 정보 ===
    
    /**
     * 현재 페이지 번호 (1-based)
     * 클라이언트에서는 1페이지부터 시작하는 것이 일반적
     */
    private int currentPage;
    
    /**
     * 전체 페이지 수
     */
    private int totalPages;
    
    /**
     * 전체 데이터 개수
     */
    private long totalElements;
    
    /**
     * 다음 페이지 존재 여부
     * 클라이언트에서 "더보기" 버튼 표시 여부 결정
     */
    private boolean hasNext;
    
    /**
     * 이전 페이지 존재 여부
     * 클라이언트에서 "이전" 버튼 표시 여부 결정
     */
    private boolean hasPrevious;
    
    /**
     * Page<ReviewResponse> → ReviewListResponse 변환 메서드
     * 
     * Spring Data JPA의 Page 객체를 클라이언트 친화적인 형태로 변환
     * 
     * Page 객체란?
     * - Spring Data JPA에서 페이징 처리 시 사용하는 컨테이너
     * - 데이터 + 페이징 메타정보를 함께 제공
     * - 0-based 페이징 (0페이지부터 시작)
     * 
     * @param reviewPage Spring Data JPA Page 객체
     * @return 클라이언트용 페이징 응답 DTO
     */
    public static ReviewListResponse from(Page<ReviewResponse> reviewPage) {
        return ReviewListResponse.builder()
                .reviews(reviewPage.getContent())               // 실제 데이터 목록
                .currentPage(reviewPage.getNumber() + 1)        // 0-based → 1-based 변환
                .totalPages(reviewPage.getTotalPages())         // 전체 페이지 수
                .totalElements(reviewPage.getTotalElements())   // 전체 데이터 개수
                .hasNext(reviewPage.hasNext())                  // 다음 페이지 존재 여부
                .hasPrevious(reviewPage.hasPrevious())          // 이전 페이지 존재 여부
                .build();
    }
    
    /**
     * 정렬 옵션과 함께 응답하는 메서드 (추후 확장용)
     * 
     * 요구사항: 최신순, 별점높은순, 별점낮은순 정렬
     * 클라이언트에서 현재 정렬 상태를 알 수 있도록 정보 제공
     */
    public static ReviewListResponse withSortInfo(Page<ReviewResponse> reviewPage, 
                                                  String sortBy, 
                                                  String sortDirection) {
        return ReviewListResponse.builder()
                .reviews(reviewPage.getContent())
                .currentPage(reviewPage.getNumber() + 1)
                .totalPages(reviewPage.getTotalPages())
                .totalElements(reviewPage.getTotalElements())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                // 추후 필요시 sortBy, sortDirection 필드 추가 가능
                .build();
    }
}
