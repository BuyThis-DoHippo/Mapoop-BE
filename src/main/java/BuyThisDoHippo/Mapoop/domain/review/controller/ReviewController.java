/**
 * 리뷰 관련 API 컨트롤러
 * 리뷰 CRUD + 태그 관리 + 통계 조회
 */
package BuyThisDoHippo.Mapoop.domain.review.controller;

import BuyThisDoHippo.Mapoop.domain.review.dto.*;
import BuyThisDoHippo.Mapoop.domain.review.service.ReviewService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController  // @Controller + @ResponseBody (JSON 응답)
@RequestMapping("/api")  // 모든 메서드에 /api 접두사 적용
@RequiredArgsConstructor  // final 필드 의존성 주입
@Slf4j  // 로깅용
public class ReviewController {

    private final ReviewService reviewService;

    // ========================================
    // 리뷰 조회 API
    // ========================================

    /**
     * 특정 화장실 리뷰 목록 조회
     * 
     * @param toiletId 화장실 ID
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 10)
     * @param sort 정렬 방식 (latest/rating_high/rating_low, 기본값: latest)
     * @return 리뷰 목록 + 페이징 정보
     * 
     * URL 예시: GET /api/toilets/1/reviews?page=1&size=10&sort=latest
     */
    @GetMapping("/toilets/{toiletId}/reviews")
    public CommonResponse<ReviewListResponse> getReviewsByToiletId(
            @PathVariable Long toiletId,  // URL 경로에서 추출: /toilets/{toiletId}
            @RequestParam(defaultValue = "1") int page,      // 쿼리 파라미터: ?page=1
            @RequestParam(defaultValue = "10") int size,     // 쿼리 파라미터: ?size=10  
            @RequestParam(defaultValue = "latest") String sort  // 쿼리 파라미터: ?sort=latest
    ) {
        log.info("화장실 리뷰 목록 조회 API 호출 - 화장실 ID: {}, 페이지: {}, 정렬: {}", toiletId, page, sort);
        
        ReviewListResponse response = reviewService.getReviewsByToiletId(toiletId, page, size, sort);
        
        return CommonResponse.onSuccess(
            response,
            "화장실 리뷰 목록 조회 성공"
        );
    }

    /**
     * 사용자 작성 리뷰 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자가 작성한 리뷰 목록
     * 
     * URL 예시: GET /api/users/123/reviews
     */
    @GetMapping("/users/{userId}/reviews")
    public CommonResponse<ReviewListResponse> getReviewsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("사용자 리뷰 목록 조회 API 호출 - 사용자 ID: {}", userId);
        
        ReviewListResponse response = reviewService.getReviewsByUserId(userId, page, size);
        
        return CommonResponse.onSuccess(
            response,
            "사용자 리뷰 목록 조회 성공"
        );
    }

    /**
     * 리뷰 상세 조회
     * 
     * @param reviewId 리뷰 ID
     * @return 리뷰 상세 정보 (태그 + 이미지 포함)
     * 
     * URL 예시: GET /api/reviews/456
     */
    @GetMapping("/reviews/{reviewId}")
    public CommonResponse<ReviewResponse> getReviewById(@PathVariable Long reviewId) {
        log.info("리뷰 상세 조회 API 호출 - 리뷰 ID: {}", reviewId);
        
        ReviewResponse response = reviewService.getReviewById(reviewId);
        
        return CommonResponse.onSuccess(
            response,
            "리뷰 상세 조회 성공"
        );
    }

    // ========================================
    // 리뷰 작성/수정/삭제 API
    // ========================================

    /**
     * 리뷰 작성
     * 
     * @param toiletId 화장실 ID  
     * @param request 리뷰 작성 요청 (별점, 내용, 태그, 이미지)
     * @param principal 인증된 사용자 정보 (Spring Security)
     * @return 작성된 리뷰 정보
     * 
     * URL 예시: POST /api/toilets/1/reviews
     * Body: {"rating": 4, "title": "깨끗해요", "content": "만족합니다", "tagIds": [1,3,5]}
     */
    @PostMapping("/toilets/{toiletId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)  // 201 Created 응답
    public CommonResponse<ReviewResponse> createReview(
            @PathVariable Long toiletId,
            @Valid @RequestBody ReviewRequest request,  // @Valid: Bean Validation 적용
            Principal principal  // 현재 로그인한 사용자 정보
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("리뷰 작성 API 호출 - 사용자 ID: {}, 화장실 ID: {}", userId, toiletId);
        
        ReviewResponse response = reviewService.createReview(userId, toiletId, request);
        
        return CommonResponse.onSuccess(
            response,
            "리뷰 작성 성공"
        );
    }

    /**
     * 리뷰 수정
     * 
     * @param reviewId 수정할 리뷰 ID
     * @param request 리뷰 수정 요청
     * @param principal 인증된 사용자 정보
     * @return 수정된 리뷰 정보
     * 
     * URL 예시: PUT /api/reviews/456
     */
    @PutMapping("/reviews/{reviewId}")
    public CommonResponse<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request,
            Principal principal
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("리뷰 수정 API 호출 - 사용자 ID: {}, 리뷰 ID: {}", userId, reviewId);
        
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        
        return CommonResponse.onSuccess(
            response,
            "리뷰 수정 성공"
        );
    }

    /**
     * 리뷰 삭제
     * 
     * @param reviewId 삭제할 리뷰 ID
     * @param principal 인증된 사용자 정보
     * @return 삭제 완료 메시지
     * 
     * URL 예시: DELETE /api/reviews/456
     */
    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 204 No Content 응답
    public CommonResponse<Void> deleteReview(
            @PathVariable Long reviewId,
            Principal principal
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("리뷰 삭제 API 호출 - 사용자 ID: {}, 리뷰 ID: {}", userId, reviewId);
        
        reviewService.deleteReview(userId, reviewId);
        
        return CommonResponse.onSuccess(null, "리뷰 삭제 성공");
    }

    // ========================================
    // 태그 관련 API
    // ========================================

    /**
     * 리뷰 작성용 태그 목록 조회
     * 
     * @return 선택 가능한 태그 목록
     * 
     * URL 예시: GET /api/tags/review
     * 응답: [{"tagId": 1, "tagName": "현재이용가능"}, {"tagId": 2, "tagName": "깨끗함"}]
     */
    @GetMapping("/tags/review")
    public CommonResponse<List<TagResponse>> getReviewTags() {
        log.info("리뷰 작성용 태그 목록 조회 API 호출");
        
        List<TagResponse> response = reviewService.getReviewTags();
        
        return CommonResponse.onSuccess(
            response,
            "리뷰 태그 목록 조회 성공"
        );
    }

    /**
     * 특정 화장실의 인기 태그 TOP 3 조회
     * 
     * @param toiletId 화장실 ID
     * @return 해당 화장실에서 가장 많이 선택된 태그 3개
     * 
     * URL 예시: GET /api/toilets/1/top-tags
     * 용도: 화장실 목록에서 각 화장실마다 대표 태그 표시
     */
    @GetMapping("/toilets/{toiletId}/top-tags")
    public CommonResponse<List<TagResponse>> getTopTagsByToiletId(@PathVariable Long toiletId) {
        log.info("화장실 인기 태그 TOP 3 조회 API 호출 - 화장실 ID: {}", toiletId);
        
        List<TagResponse> response = reviewService.getTopTagsByToiletId(toiletId);
        
        return CommonResponse.onSuccess(
            response,
            "화장실 인기 태그 조회 성공"
        );
    }

    // ========================================
    // 통계 조회 API
    // ========================================

    /**
     * 특정 화장실의 평균 별점 조회
     * 
     * @param toiletId 화장실 ID
     * @return 평균 별점 (소수점 1자리)
     * 
     * URL 예시: GET /api/toilets/1/rating
     */
    @GetMapping("/toilets/{toiletId}/rating")
    public CommonResponse<Double> getAverageRating(@PathVariable Long toiletId) {
        log.info("화장실 평균 별점 조회 API 호출 - 화장실 ID: {}", toiletId);
        
        Double averageRating = reviewService.getAverageRating(toiletId);
        
        return CommonResponse.onSuccess(
            averageRating,
            "평균 별점 조회 성공"
        );
    }

    /**
     * 특정 화장실의 리뷰 개수 조회
     * 
     * @param toiletId 화장실 ID
     * @return 리뷰 개수
     * 
     * URL 예시: GET /api/toilets/1/review-count
     */
    @GetMapping("/toilets/{toiletId}/review-count")
    public CommonResponse<Long> getReviewCount(@PathVariable Long toiletId) {
        log.info("화장실 리뷰 개수 조회 API 호출 - 화장실 ID: {}", toiletId);
        
        Long reviewCount = reviewService.getReviewCount(toiletId);
        
        return CommonResponse.onSuccess(
            reviewCount,
            "리뷰 개수 조회 성공"
        );
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * Principal에서 사용자 ID 추출
     * 
     * Spring Security에서 인증된 사용자 정보를 Principal 객체로 제공
     * JWT 토큰에서 사용자 ID를 추출하는 로직
     * 
     * @param principal 인증된 사용자 정보
     * @return 사용자 ID
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        // Principal이 null인 경우 (인증되지 않은 요청)
        if (principal == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
        
        // 실제 구현에서는 JWT 토큰에서 사용자 ID 추출
        // 예시: JwtUtils.getUserIdFromToken(principal.getName())
        
        // 임시로 principal.getName()을 Long으로 변환
        // 실제로는 JwtAuthenticationFilter에서 설정한 값 사용
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("Principal에서 사용자 ID 추출 실패: {}", principal.getName(), e);
            throw new IllegalArgumentException("유효하지 않은 사용자 정보입니다.");
        }
    }
}