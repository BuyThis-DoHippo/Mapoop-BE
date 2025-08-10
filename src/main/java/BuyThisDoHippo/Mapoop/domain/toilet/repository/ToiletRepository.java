package BuyThisDoHippo.Mapoop.domain.toilet.repository;

import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.projection.ToiletWithDistance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToiletRepository extends JpaRepository<Toilet, Long> {
    List<Toilet> findByNameContainingIgnoreCaseOrderByAvgRatingDesc(String keyword, Pageable pageable);

    List<Toilet> findAllByOrderByAvgRatingDesc(Pageable pageable);

    @Query(value = """

            SELECT 
               t.id as id,
               t.name as name,
               t.type as type,
               t.is_partnership as isPartnership,
               t.latitude as latitude,
               t.longitude as longitude,
               t.address as address,
               t.floor as floor,
               t.avg_rating as avgRating,
               t.total_reviews as totalReviews,
               (6371 * acos(cos(radians(:userLat)) * cos(radians(t.latitude)) 
                          * cos(radians(t.longitude) - radians(:userLng)) 
                          + sin(radians(:userLat)) * sin(radians(t.latitude)))) AS distance
        FROM toilet t
        HAVING distance <= :radiusKm 
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<ToiletWithDistance> findNearbyToilets(@Param("userLat") double userLatitude,
                                               @Param("userLng") double userLongitude,
                                               @Param("radiusKm") double radiusKm,
                                               @Param("limit") int limit);

    @Query("SELECT tt FROM ToiletTag tt JOIN FETCH tt.tag WHERE tt.toilet.id IN :toiletIds")
    List<ToiletTag> findTagsByToiletIds(@Param("toiletIds") List<Long> toiletIds);

}