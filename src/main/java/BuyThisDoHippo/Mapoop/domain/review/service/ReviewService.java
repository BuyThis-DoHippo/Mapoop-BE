package BuyThisDoHippo.Mapoop.domain.review.service;


import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.image.repository.ImageRepository;
import BuyThisDoHippo.Mapoop.domain.image.repository.ReviewImageRepository;
import BuyThisDoHippo.Mapoop.domain.image.service.S3ImageService;
import BuyThisDoHippo.Mapoop.domain.review.dto.ReviewListResponse;
import BuyThisDoHippo.Mapoop.domain.review.dto.ReviewRequest;
import BuyThisDoHippo.Mapoop.domain.review.dto.ReviewResponse;
import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.domain.review.entity.ReviewType;
import BuyThisDoHippo.Mapoop.domain.review.repository.ReviewRepository;
import BuyThisDoHippo.Mapoop.domain.review.dto.TagResponse;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ReviewTag;
import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.ReviewTagRepository;
import BuyThisDoHippo.Mapoop.domain.tag.repository.TagRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.domain.user.repository.UserRepository;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ToiletRepository toiletRepository;
    private final TagRepository tagRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final ImageRepository imageRepository;
    private final ReviewImageRepository reviewImageRepository;

    @Autowired(required = false)
    private S3ImageService s3ImageService;


    /**
     * 특정 화장실 리뷰 목록 조회
     */
    public ReviewListResponse getReviewsByToiletId(Long toiletId, int page, int size, String sort) {
        log.info("화장실 리뷰 목록 조회 - 화장실 ID: {}, 페이지: {}", toiletId, page);

        findToiletById(toiletId);

        Sort sortCondition = createSortCondition(sort);
        Pageable pageable = PageRequest.of(page - 1, size, sortCondition);

        Page<Review> reviews = reviewRepository.findByToiletIdAndTypeOrderByCreatedAtDesc(
                toiletId, ReviewType.ACTIVE, pageable);

        Page<ReviewResponse> reviewResponses = reviews.map(this::convertToResponseWithTags);

        return ReviewListResponse.from(reviewResponses);
    }

    /**
     * 사용자의 리뷰 목록 조회
     */
    public ReviewListResponse getReviewsByUserId(Long userId, int page, int size) {
        log.info("사용자 리뷰 목록 조회 - 사용자 ID: {}, 페이지: {}", userId, page);

        findUserById(userId);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Review> reviews = reviewRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                userId, ReviewType.ACTIVE, pageable);

        Page<ReviewResponse> reviewResponses = reviews.map(this::convertToResponseWithTags);
        return ReviewListResponse.from(reviewResponses);
    }

    /**
     * 특정 리뷰 상세 조회 (태그 + 이미지 포함)
     */
    public ReviewResponse getReviewById(Long reviewId) {
        log.info("리뷰 상세 조회 - 리뷰 ID: {}", reviewId);

        Review review = findReviewById(reviewId);

        if (review.getType() != ReviewType.ACTIVE) {
            throw new ApplicationException(CustomErrorCode.REVIEW_NOT_FOUND);
        }

        return convertToResponseWithTags(review);
    }
    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewResponse createReview(Long userId, Long toiletId, ReviewRequest request) {
        log.info("리뷰 작성 - 사용자 ID: {}, 화장실 ID: {}", userId, toiletId);

        User user = findUserById(userId);
        Toilet toilet = findToiletById(toiletId);

        boolean hasReview = reviewRepository.existsByUserIdAndToiletIdAndType(
                userId, toiletId, ReviewType.ACTIVE);
        if(hasReview) {
            throw new ApplicationException(CustomErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .user(user)
                .toilet(toilet)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Review savedReview = reviewRepository.save(review);

        saveReviewTags(savedReview, request.getTagIds());

        updateToiletRating(toiletId);

        log.info("리뷰 작성 완료 - 리뷰 ID: {}", savedReview.getId());

        return convertToResponseWithTags(savedReview);
    }

    /**
     * 이미지 포함 리뷰 작성
     */
        @Transactional
        public ReviewResponse createReviewWithImages(Long userId, Long toiletId, ReviewRequest request, List<MultipartFile> images) {
            log.info("이미지 포함 리뷰 작성 - 사용자 ID: {}, 화장실 ID: {}", userId, toiletId);

            User user = findUserById(userId);
            Toilet toilet = findToiletById(toiletId);

            // 중복 리뷰 체크
            boolean hasReview = reviewRepository.existsByUserIdAndToiletIdAndType(userId, toiletId, ReviewType.ACTIVE);
            if(hasReview) {
                throw new ApplicationException(CustomErrorCode.REVIEW_ALREADY_EXISTS);
            }

            // 리뷰 생성
            Review review = Review.builder()
                    .user(user)
                    .toilet(toilet)
                    .rating(request.getRating())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .build();

            Review savedReview = reviewRepository.save(review);

            // 이미지 업로드 및 저장
            if (images != null && !images.isEmpty()) {
                if (s3ImageService != null) {
                    List<String> imageUrls = s3ImageService.uploadReviewImages(savedReview.getId(), images);
                    log.info("[S3 업로드 결과] 업로드된 URL 개수: {}", imageUrls.size());
                    saveReviewImages(savedReview, images, imageUrls);
                } else {
                    log.warn("S3ImageService가 비활성화되어 있어 이미지 업로드를 건너뜁니다.");
                }
            }

            // 태그 저장
            saveReviewTags(savedReview, request.getTagIds());

            // 화장실 평점 업데이트
            updateToiletRating(toiletId);

            log.info("이미지 포함 리뷰 작성 완료 - 리뷰 ID: {}", savedReview.getId());

            return convertToResponseWithTagsAndImages(savedReview.getId());
        }

    /**
     * 리뷰 이미지 정보 저장
     */
    private void saveReviewImages(Review review, List<MultipartFile> imageFiles, List<String> imageUrls) {
        List<Image> images = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            MultipartFile file = imageFiles.get(i);
            String imageUrl = imageUrls.get(i);
            String s3Key = (s3ImageService != null) ?
                s3ImageService.extractS3KeyFromUrl(imageUrl) : "default-key";

            Image image = Image.createReviewImage(
                    imageUrl,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    s3Key,
                    review
            );

            images.add(image);
        }

        imageRepository.saveAll(images);
        log.info("리뷰 이미지 정보 저장 완료 - 리뷰 ID: {}, 이미지 개수: {}", review.getId(), images.size());
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request) {
        log.info("리뷰 수정 - 사용자 ID: {}, 리뷰 ID: {}", userId, reviewId);

        Review review = findReviewById(reviewId);
        if (!review.isWrittenBy(userId)) {
            throw new ApplicationException(CustomErrorCode.UNAUTHORIZED);
        }

        review.updateReview(request.getRating(), request.getTitle(), request.getContent());

        reviewTagRepository.deleteByReviewId(reviewId);
        saveReviewTags(review, request.getTagIds());

        updateToiletRating(review.getToilet().getId());

        log.info("리뷰 수정 완료 - 리뷰 ID: {}", reviewId);

        return convertToResponseWithTags(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        log.info("리뷰 삭제 - 사용자 ID: {}, 리뷰 ID: {}", userId, reviewId);

        Review review = findReviewById(reviewId);

        if (!review.isWrittenBy(userId)) {
            throw new ApplicationException(CustomErrorCode.UNAUTHORIZED);
        }

        Long toiletId = review.getToilet().getId();
        review.deleteReview();

        reviewTagRepository.deleteByReviewId(reviewId);

        updateToiletRating(toiletId);

        log.info("리뷰 삭제 완료 - 리뷰 ID: {}", reviewId);
    }



    public List<TagResponse> getReviewTags() {
        log.info("리뷰 작성용 태그 목록 조회");

        List<Tag> tags = tagRepository.findReviewTags();

        return tags.stream()
                .map(TagResponse::from)
                .collect(Collectors.toList());
    }

    public List<TagResponse> getTopTagsByToiletId(Long toiletId) {
        log.info("화장실 인기 태그 TOP 3 조회 - 화장실 ID: {}", toiletId);

        List<Tag> topTags = reviewTagRepository.findTop3TagsByToiletId(toiletId);

        return topTags.stream()
                .map(TagResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 화장실의 평균 별점 조회
     */
    public Double getAverageRating(Long toiletId) {
        Double averageRating = reviewRepository.findAverageRatingByToiletId(toiletId);
        return averageRating != null ? Math.round(averageRating * 10) / 10.0 : 0.0;
    }

    /**
     * 특정 화장실의 리뷰 개수 조회
     */
    public Long getReviewCount(Long toiletId) {
        return reviewRepository.countByToiletIdAndType(toiletId, ReviewType.ACTIVE);
    }

    /**
     * 탑3 태그에 특정 태그가 포함된 화장실 ID 목록 조회
     * 검색 필터링에서 상태 태그 기반으로 화장실을 찾을 때 사용
     */
    public List<Long> getToiletIdsByTopTag(String tagName) {
        log.info("탑3 태그 기준 화장실 조회 - 태그명: {}", tagName);
        
        // 간단한 버전 사용: 해당 태그가 2번 이상 언급된 화장실
        List<Long> toiletIds = reviewTagRepository.findToiletIdsWithTopTagSimple(tagName);
        
        log.info("태그 '{}' 기준 화장실 개수: {}", tagName, toiletIds.size());
        return toiletIds;
    }

    /**
     * 여러 탑3 태그가 모두 포함된 화장실 ID 목록 조회
     * 다중 상태 태그 필터링에서 사용
     */
    public List<Long> getToiletIdsByTopTags(List<String> tagNames) {
        log.info("다중 탑3 태그 기준 화장실 조회 - 태그명들: {}", tagNames);
        
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> toiletIds = reviewTagRepository.findToiletIdsWithAllTopTags(tagNames, tagNames.size());
        
        log.info("탑3 태그에 모든 태그({})가 포함된 화장실 개수: {}", tagNames, toiletIds.size());
        return toiletIds;
    }

    private ReviewResponse convertToResponseWithTags(Review review) {
        List<TagResponse> tags = getTagsByReviewId(review.getId());
        
        // 이미지 URL 직접 조회 (Repository 사용)
        List<String> imageUrls = imageRepository.findByReview_IdOrderByCreatedAtAsc(review.getId())
                .stream()
                .map(Image::getImageUrl)
                .collect(Collectors.toList());
        
        return ReviewResponse.withTagsAndImages(review, tags, imageUrls);
    }

    private ReviewResponse convertToResponseWithTagsAndImages(Long reviewId) {
        Review r = reviewRepository.findById(reviewId).orElseThrow();
        List<String> urls = imageRepository.findByReview_IdOrderByCreatedAtAsc(r.getId()) // r.getId() 사용
                .stream()
                .map(Image::getImageUrl) // Image 엔티티의 imageUrl 필드 사용
                .collect(Collectors.toList());

        // 기존 convertToResponseWithTags(...) 있으면 그걸 확장하거나 여기서 직접 빌더로 만들기
        ReviewResponse resp = convertToResponseWithTags(r);
        resp.setImageUrls(urls); // 세터가 없으면 빌더/생성자에 imageUrls 추가
        return resp;
    }

    private List<TagResponse> getTagsByReviewId(Long reviewId) {
        List<ReviewTag> reviewTags = reviewTagRepository.findByReviewId(reviewId);

        return reviewTags.stream()
                .map(reviewTag -> TagResponse.from(reviewTag.getTag()))
                .collect(Collectors.toList());
    }

    private void saveReviewTags(Review review, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            log.info("태그 ID 목록이 비어있습니다. 태그 저장을 건너뜁니다.");
            return;
        }

        log.info("태그 저장 시작 - 태그 ID 목록: {}", tagIds);

        try {
            List<Tag> tags = tagRepository.findByIdIn(tagIds);
            log.info("조회된 태그 개수: {}, 요청된 태그 ID 개수: {}", tags.size(), tagIds.size());
            
            // 유효하지 않은 태그 ID가 있는지 확인
            if (tags.size() != tagIds.size()) {
                log.error("일부 태그를 찾을 수 없습니다. 요청: {}, 조회: {}", tagIds.size(), tags.size());
                throw new ApplicationException(CustomErrorCode.TAG_NOT_FOUND);
            }

            // ReviewTag 엔티티들 생성 및 저장
            List<ReviewTag> reviewTags = tags.stream()
                    .map(tag -> ReviewTag.builder()
                            .review(review)
                            .tag(tag)
                            .build())
                    .collect(Collectors.toList());
            
            reviewTagRepository.saveAll(reviewTags);
            
            log.info("리뷰 태그 저장 완료 - 리뷰 ID: {}, 태그 개수: {}", review.getId(), reviewTags.size());
        } catch (Exception e) {
            log.error("태그 저장 중 오류 발생: ", e);
            throw e;
        }
    }

    private Sort createSortCondition(String sort) {
        return switch (sort) {
            case "rating_high" -> Sort.by(Sort.Direction.DESC, "rating")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "rating_low" -> Sort.by(Sort.Direction.ASC, "rating")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private void updateToiletRating(Long toiletId) {
        Double averageRating = getAverageRating(toiletId);

        Toilet toilet = findToiletById(toiletId);
        toilet.updateAverageRating(averageRating);

        log.info("화장실 평균 별점 업데이트 - 화장실 ID: {}, 평균 별점: {}", toiletId, averageRating);
    }

    /**
     * 사용자 조회
     */
    private BuyThisDoHippo.Mapoop.domain.user.entity.User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.USER_NOT_FOUND));
    }

    private Toilet findToiletById(Long toiletId) {
        return toiletRepository.findById(toiletId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.TOILET_NOT_FOUND));
    }

    private Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.REVIEW_NOT_FOUND));
    }
}
