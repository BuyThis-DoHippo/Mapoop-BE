package BuyThisDoHippo.Mapoop.domain.image.repository;

import BuyThisDoHippo.Mapoop.domain.image.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    List<ReviewImage> findAllByReviewId(Long reviewId);
    void deleteByReviewId(Long reviewId);
}