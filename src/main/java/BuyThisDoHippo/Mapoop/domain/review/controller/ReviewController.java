/**
 * 리뷰 관련 API 컨트롤러
 * 리뷰 CRUD + 태그 관리 + 통계 조회
 */
package BuyThisDoHippo.Mapoop.domain.review.controller;

import BuyThisDoHippo.Mapoop.domain.image.service.ReviewImageService;
import BuyThisDoHippo.Mapoop.domain.review.dto.*;
import BuyThisDoHippo.Mapoop.domain.review.service.ReviewService;
import BuyThisDoHippo.Mapoop.global.auth.JwtUtils;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController  // @Controller + @ResponseBody (JSON 응답)
@RequestMapping("/api")  // 모든 메서드에 /api 접두사 적용
@RequiredArgsConstructor  // final 필드 의존성 주입
@Slf4j  // 로깅용
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewImageService reviewImageService;

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
     * 이미지 포함 리뷰 작성
     */
    @PostMapping(value = "/toilets/{toiletId}/reviews/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<ReviewResponse> createReviewWithImages(
            @PathVariable Long toiletId,
            @RequestPart("review") @Valid ReviewRequest request,  // JSON 데이터
            @RequestPart(value = "images", required = false) List<MultipartFile> images,  // 이미지 파일들
            Principal principal
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("이미지 포함 리뷰 작성 API 호출 - 사용자 ID: {}, 화장실 ID: {}, 이미지 개수: {}",
                userId, toiletId, images != null ? images.size() : 0);

        ReviewResponse response = reviewService.createReviewWithImages(userId, toiletId, request, images);

        return CommonResponse.onSuccess(response, "리뷰 작성 성공");
    }

    @PostMapping(value = "/toilets/{toiletId}/reviews/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<List<String>> uploadReviewImages(
            @PathVariable Long toiletId, // 화장실 ID 받음!
            @RequestPart("images") List<MultipartFile> images, // 이미지 파일들
            Principal principal // 누가 올렸는지 알기 위해 필요함
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        log.info("리뷰 이미지 개별 업로드 API 호출 - 사용자 ID: {}, 화장실 ID: {}, 이미지 개수: {}",
                userId, toiletId, images != null ? images.size() : 0);

        // TODO: 여기서는 파일들을 저장하고 저장된 URL 리스트를 반환한다고 가정
        // reviewImageService 에 toiletId까지 넘겨줘서 필요하다면 활용하게 할 수 있어.
        List<String> imageUrls = reviewImageService.uploadReviewImages(userId, toiletId, images);

        return CommonResponse.onSuccess(imageUrls, "리뷰 이미지 업로드 성공");
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

    /**
     * 특정 화장실의 평점 분포 조회
     * 
     * @param toiletId 화장실 ID
     * @return 평점별 개수와 비율 분포
     * 
     * URL 예시: GET /api/toilets/1/rating-distribution
     */
    @GetMapping("/toilets/{toiletId}/rating-distribution")
    public CommonResponse<Object> getRatingDistribution(@PathVariable Long toiletId) {
        log.info("화장실 평점 분포 조회 API 호출 - 화장실 ID: {}", toiletId);
        
        Object distribution = reviewService.getRatingDistribution(toiletId);
        
        return CommonResponse.onSuccess(
            distribution,
            "평점 분포 조회 성공"
        );
    }

    // ========================================
    // 검색 필터링용 API (검색 도메인에서 사용)
    // ========================================

    /**
     * 탑3 태그에 특정 태그가 포함된 화장실 ID 목록 조회
     * 검색 도메인에서 상태 태그 필터링 시 사용
     * 
     * @param tagName 태그명 (예: "깨끗함", "향기좋음")
     * @return 해당 태그가 탑3에 포함된 화장실 ID 목록
     * 
     * URL 예시: GET /api/toilets/by-top-tag?tagName=깨끗함
     */
    @GetMapping("/toilets/by-top-tag")
    public CommonResponse<List<Long>> getToiletIdsByTopTag(@RequestParam String tagName) {
        log.info("탑3 태그 기준 화장실 조회 API 호출 - 태그명: {}", tagName);
        
        List<Long> toiletIds = reviewService.getToiletIdsByTopTag(tagName);
        
        return CommonResponse.onSuccess(
            toiletIds,
            "탑3 태그 기준 화장실 조회 성공"
        );
    }

    /**
     * 여러 탑3 태그가 모두 포함된 화장실 ID 목록 조회
     * 다중 상태 태그 필터링 시 사용
     * 
     * @param tagNames 태그명 목록 (예: ["깨끗함", "향기좋음"])
     * @return 모든 태그가 탑3에 포함된 화장실 ID 목록
     * 
     * URL 예시: GET /api/toilets/by-top-tags?tagNames=깨끗함,향기좋음
     */
    @GetMapping("/toilets/by-top-tags")
    public CommonResponse<List<Long>> getToiletIdsByTopTags(@RequestParam List<String> tagNames) {
        log.info("다중 탑3 태그 기준 화장실 조회 API 호출 - 태그명들: {}", tagNames);
        
        List<Long> toiletIds = reviewService.getToiletIdsByTopTags(tagNames);
        
        return CommonResponse.onSuccess(
            toiletIds,
            "다중 탑3 태그 기준 화장실 조회 성공"
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
    /**
     * Principal에서 사용자 ID 추출
     * 
     * @param principal 인증된 사용자 정보
     * @return 사용자 ID
     * @throws ApplicationException 토큰이 없거나 유효하지 않은 경우
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            log.warn("Principal이 null입니다. 인증되지 않은 요청");
            throw new ApplicationException(CustomErrorCode.MISSING_TOKEN);
        }

        try {
            // JwtAuthenticationFilter에서 이미 userId를 설정했으므로 그대로 사용
            Long userId = Long.parseLong(principal.getName());
            log.debug("Principal에서 사용자 ID 추출 성공: {}", userId);
            return userId;
        } catch (NumberFormatException e) {
            log.error("Principal에서 사용자 ID 추출 실패 - 잘못된 형식: {}", principal.getName(), e);
            throw new ApplicationException(CustomErrorCode.INVALID_TOKEN);
        }
    }

}