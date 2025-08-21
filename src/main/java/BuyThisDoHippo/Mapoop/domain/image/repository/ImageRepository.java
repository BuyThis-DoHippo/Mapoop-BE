package BuyThisDoHippo.Mapoop.domain.image.repository;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.image.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * 리뷰 ID로 이미지 목록 조회
     */
    List<Image> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

    /**
     * 화장실 ID로 이미지 목록 조회
     */
    List<Image> findByToiletIdOrderByCreatedAtAsc(Long toiletId);

    /**
     * 타겟 타입과 타겟 ID로 이미지 목록 조회
     */
    List<Image> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(TargetType targetType, Long targetId);

    /**
     * 리뷰의 이미지 개수 조회
     */
    Long countByReviewId(Long reviewId);

    /**
     * 화장실의 이미지 개수 조회
     */
    Long countByToiletId(Long toiletId);

    /**
     * S3 키로 이미지 조회
     */
    Image findByS3Key(String s3Key);

    /**
     * 리뷰 삭제 시 해당 리뷰의 모든 이미지 삭제
     */
    void deleteByReviewId(Long reviewId);

    /**
     * 화장실 삭제 시 해당 화장실의 모든 이미지 삭제
     */
    void deleteByToiletId(Long toiletId);

    /**
     * 리뷰의 첫 번째 이미지 조회 (썸네일용)
     */
    @Query("SELECT i FROM Image i WHERE i.review.id = :reviewId ORDER BY i.createdAt ASC LIMIT 1")
    Image findFirstImageByReviewId(@Param("reviewId") Long reviewId);

    /**
     * 화장실의 첫 번째 이미지 조회 (썸네일용)
     */
    @Query("SELECT i FROM Image i WHERE i.toilet.id = :toiletId ORDER BY i.createdAt ASC LIMIT 1")
    Image findFirstImageByToiletId(@Param("toiletId") Long toiletId);

    /**
     * 특정 사용자가 업로드한 이미지들 조회 (리뷰를 통해)
     */
    @Query("SELECT i FROM Image i WHERE i.review.user.id = :userId ORDER BY i.createdAt DESC")
    List<Image> findImagesByUserId(@Param("userId") Long userId);

    /**
     * 이미지 URL 목록으로 이미지들 조회
     */
    List<Image> findByImageUrlIn(List<String> imageUrls);
}
