package BuyThisDoHippo.Mapoop.domain.image.repository;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

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
    Optional<Image> findByS3Key(String s3Key);

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

    /** 화장실 관련 */
    // 화장실의 이미지 목록 (생성일 오름차순)
    List<Image> findByToilet_IdOrderByCreatedAtAsc(Long toiletId);

    // 화장실의 이미지 개수
    long countByToilet_Id(Long toiletId);

    // 화장실의 모든 이미지 삭제 (DB 레코드)
    void deleteByToilet_Id(Long toiletId);

    // 화장실의 첫 번째 이미지(썸네일)
    Optional<Image> findFirstByToilet_IdOrderByCreatedAtAsc(Long toiletId);

    @Query("""
        select i.imageUrl
          from Image i
         where i.toilet.id = :toiletId
         order by i.createdAt asc
    """)
    List<String> findToiletImageUrls(@Param("toiletId") Long toiletId);

    /** 리뷰 관련 */
    // 리뷰의 이미지 목록 (생성일 오름차순)
    List<Image> findByReview_IdOrderByCreatedAtAsc(Long reviewId);

    // 리뷰의 이미지 개수
    long countByReview_Id(Long reviewId);

    // 리뷰의 모든 이미지 삭제 (DB 레코드)
    void deleteByReview_Id(Long reviewId);

    // 리뷰의 첫 번째 이미지(썸네일)
    Optional<Image> findFirstByReview_IdOrderByCreatedAtAsc(Long reviewId);

    @Query("""
        select i.imageUrl
          from Image i
         where i.review.id = :reviewId
         order by i.createdAt asc
    """)
    List<String> findReviewImageUrls(@Param("reviewId") Long reviewId);
}
