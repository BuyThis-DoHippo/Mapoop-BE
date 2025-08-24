package BuyThisDoHippo.Mapoop.domain.review.repository;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.domain.review.entity.ReviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByToiletIdAndTypeOrderByCreatedAtDesc(
            Long toiletId, ReviewType type, Pageable pageable);

    Page<Review> findByUserIdAndTypeOrderByCreatedAtDesc(
            Long userId, ReviewType type, Pageable pageable
    );

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.toilet.id = :toiletId AND r.type = 'ACTIVE'")
    Double findAverageRatingByToiletId(@Param("toiletId") Long toiletId);

    Long countByToiletIdAndType(Long toiletId, ReviewType type);

    @Query("SELECT r FROM Review r WHERE r.user.id = :userId AND  r.toilet.id = :toiletId AND r.type = 'ACTIVE'")
    Review findByUserIdAndToiletId(@Param("userId") Long userId, @Param("toiletId") Long toiletId);

    boolean existsByUserIdAndToiletIdAndType(Long userId, Long toiletId, ReviewType type);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.toilet.id = :toiletId AND r.type = 'ACTIVE' GROUP BY r.rating ORDER BY r.rating DESC")
    Object[] findRatingStatisticsByToiletId(@Param("toiletId") Long toiletId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.toilet.id = :toiletId AND r.type = 'ACTIVE' GROUP BY r.rating ORDER BY r.rating DESC")
    java.util.List<Object[]> countByRatingGroupByToiletId(@Param("toiletId") Long toiletId);

    boolean existsByIdAndUserId(Long reviewId, Long userId);
}
