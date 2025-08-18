package BuyThisDoHippo.Mapoop.domain.toilet.repository;

import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerDto;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.GenderType;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.projection.ToiletWithDistance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ToiletRepository extends JpaRepository<Toilet, Long> {
    List<Toilet> findByNameContainingIgnoreCaseOrderByAvgRatingDesc(String keyword, Pageable pageable);

    List<Toilet> findAllByOrderByAvgRatingDesc(Pageable pageable);

    // 임시: 빈 결과 반환 (쿼리 생성 안 함)
    default List<MarkerDto> findMarkers(
            Double minRating,
            ToiletType type,
            GenderType genderType,
            Boolean a, Boolean b, Boolean c, Boolean d, Boolean e, Boolean f, Boolean g,
            LocalTime now
    ) {
        return java.util.Collections.emptyList();
    }

    // 임시: 0 반환
    default long countMarkers(
            Double minRating,
            ToiletType type,
            GenderType genderType,
            Boolean a, Boolean b, Boolean c, Boolean d, Boolean e, Boolean f, Boolean g,
            LocalTime now
    ) {
        return 0L;
    }

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
       (6371 * acos(
            cos(radians(:userLat)) * cos(radians(t.latitude)) 
          * cos(radians(t.longitude) - radians(:userLng)) 
          + sin(radians(:userLat)) * sin(radians(t.latitude))
       ) * 1000) AS distance
    FROM toilet t
    HAVING (distance / 1000) <= :radiusKm
    ORDER BY distance
    LIMIT :limit
    """, nativeQuery = true)
    List<ToiletWithDistance> findNearbyToilets(@Param("userLat") double userLatitude,
                                               @Param("userLng") double userLongitude,
                                               @Param("radiusKm") double radiusKm,
                                               @Param("limit") int limit);

    @Query("SELECT tt FROM ToiletTag tt JOIN FETCH tt.tag WHERE tt.toilet.id IN :toiletIds")
    List<ToiletTag> findTagsByToiletIds(@Param("toiletIds") List<Long> toiletIds);

    // 거리순으로 search
    @Query(
            value = """
              SELECT
                t.id,
                t.name,
                t.latitude,
                t.longitude,
                t.address,
                t.floor,
                t.avg_rating   AS avgRating,
                t.total_reviews AS totalReviews,
                (6371000 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(t.latitude)) *
                    COS(RADIANS(t.longitude) - RADIANS(:lng)) +
                    SIN(RADIANS(:lat)) * SIN(RADIANS(t.latitude))
                )) AS distance
              FROM toilet t
              WHERE
                (:keyword IS NULL OR t.name LIKE CONCAT('%', :keyword, '%'))
                AND (:minRating IS NULL OR (t.avg_rating IS NOT NULL AND t.avg_rating >= :minRating))
                AND (:type IS NULL OR t.type = :type)
                AND (:genderType IS NULL OR t.gender_type = :genderType)
                AND (:isOpen24h IS NULL OR t.open_24h = :isOpen24h)
                AND (:hasIndoorToilet IS NULL OR t.has_indoor_toilet = :hasIndoorToilet)
                AND (:hasBidet IS NULL OR t.has_bidet = :hasBidet)
                AND (:providesSanitaryItems IS NULL OR t.provides_sanitary_items = :providesSanitaryItems)
                AND (:hasAccessibleToilet IS NULL OR t.has_accessible_toilet = :hasAccessibleToilet)
                AND (:hasDiaperTable IS NULL OR t.has_diaper_table = :hasDiaperTable)
                AND (
                  :isAvailable IS NULL
                  OR (
                        CASE
                          WHEN t.open_24h = 1 THEN 1
                          WHEN t.open_time IS NULL OR t.close_time IS NULL THEN 0
                          WHEN t.open_time < t.close_time
                               THEN (TIME(:now) BETWEEN t.open_time AND t.close_time)
                          ELSE
                               (TIME(:now) >= t.open_time OR TIME(:now) < t.close_time)
                        END
                     ) = :isAvailable
                )
              ORDER BY
                distance ASC,
                (t.avg_rating IS NULL) ASC,
                t.avg_rating DESC,
                t.total_reviews DESC
              """,
                        countQuery = """
              SELECT COUNT(*)
              FROM toilet t
              WHERE
                (:keyword IS NULL OR t.name LIKE CONCAT('%', :keyword, '%'))
                AND (:minRating IS NULL OR (t.avg_rating IS NOT NULL AND t.avg_rating >= :minRating))
                AND (:type IS NULL OR t.type = :type)
                AND (:genderType IS NULL OR t.gender_type = :genderType)
                AND (:isOpen24h IS NULL OR t.open_24h = :isOpen24h)
                AND (:hasIndoorToilet IS NULL OR t.has_indoor_toilet = :hasIndoorToilet)
                AND (:hasBidet IS NULL OR t.has_bidet = :hasBidet)
                AND (:providesSanitaryItems IS NULL OR t.provides_sanitary_items = :providesSanitaryItems)
                AND (:hasAccessibleToilet IS NULL OR t.has_accessible_toilet = :hasAccessibleToilet)
                AND (:hasDiaperTable IS NULL OR t.has_diaper_table = :hasDiaperTable)
                AND (
                  :isAvailable IS NULL
                  OR (
                        CASE
                          WHEN t.open_24h = 1 THEN 1
                          WHEN t.open_time IS NULL OR t.close_time IS NULL THEN 0
                          WHEN t.open_time < t.close_time
                               THEN (TIME(:now) BETWEEN t.open_time AND t.close_time)
                          ELSE
                               (TIME(:now) >= t.open_time OR TIME(:now) < t.close_time)
                        END
                     ) = :isAvailable
                )
              """,
            nativeQuery = true
    )
    Page<ToiletWithDistance> searchByDistance(
            @Param("keyword") String keyword,
            @Param("minRating") Double minRating,
            @Param("type") String type,                // 'PUBLIC' or 'STORE'
            @Param("genderType") String genderType,    // 'UNISEX' or 'SEPARATE'
            @Param("isOpen24h") Boolean isOpen24h,
            @Param("hasIndoorToilet") Boolean hasIndoorToilet,
            @Param("hasBidet") Boolean hasBidet,
            @Param("hasAccessibleToilet") Boolean hasAccessibleToilet,
            @Param("hasDiaperTable") Boolean hasDiaperTable,
            @Param("providesSanitaryItems") Boolean providesSanitaryItems,
            @Param("isAvailable") Boolean isAvailable,
            @Param("now") String nowTime,              // "HH:mm:ss"
            @Param("lat") double lat,
            @Param("lng") double lng,
            Pageable pageable
    );

    // 위치 없음 → 평점순
    @Query(
            value = """
              SELECT
                t.id,
                t.name,
                t.latitude,
                t.longitude,
                t.address,
                t.floor,
                t.avg_rating   AS avgRating,
                t.total_reviews AS totalReviews,
                NULL AS distance
              FROM toilet t
              WHERE
                (:keyword IS NULL OR t.name LIKE CONCAT('%', :keyword, '%'))
                AND (:minRating IS NULL OR (t.avg_rating IS NOT NULL AND t.avg_rating >= :minRating))
                AND (:type IS NULL OR t.type = :type)
                AND (:genderType IS NULL OR t.gender_type = :genderType)
                AND (:isOpen24h IS NULL OR t.open_24h = :isOpen24h)
                AND (:hasIndoorToilet IS NULL OR t.has_indoor_toilet = :hasIndoorToilet)
                AND (:hasBidet IS NULL OR t.has_bidet = :hasBidet)
                AND (:providesSanitaryItems IS NULL OR t.provides_sanitary_items = :providesSanitaryItems)
                AND (:hasAccessibleToilet IS NULL OR t.has_accessible_toilet = :hasAccessibleToilet)
                AND (:hasDiaperTable IS NULL OR t.has_diaper_table = :hasDiaperTable)
                AND (
                  :isAvailable IS NULL
                  OR (
                        CASE
                          WHEN t.open_24h = 1 THEN 1
                          WHEN t.open_time IS NULL OR t.close_time IS NULL THEN 0
                          WHEN t.open_time < t.close_time
                               THEN (TIME(:now) BETWEEN t.open_time AND t.close_time)
                          ELSE
                               (TIME(:now) >= t.open_time OR TIME(:now) < t.close_time)
                        END
                     ) = :isAvailable
                )
              ORDER BY
                (t.avg_rating IS NULL) ASC,
                t.avg_rating DESC,
                t.total_reviews DESC
              """,
                        countQuery = """
              SELECT COUNT(*)
              FROM toilet t
              WHERE
                (:keyword IS NULL OR t.name LIKE CONCAT('%', :keyword, '%'))
                AND (:minRating IS NULL OR (t.avg_rating IS NOT NULL AND t.avg_rating >= :minRating))
                AND (:type IS NULL OR t.type = :type)
                AND (:genderType IS NULL OR t.gender_type = :genderType)
                AND (:isOpen24h IS NULL OR t.open_24h = :isOpen24h)
                AND (:hasIndoorToilet IS NULL OR t.has_indoor_toilet = :hasIndoorToilet)
                AND (:hasBidet IS NULL OR t.has_bidet = :hasBidet)
                AND (:providesSanitaryItems IS NULL OR t.provides_sanitary_items = :providesSanitaryItems)
                AND (:hasAccessibleToilet IS NULL OR t.has_accessible_toilet = :hasAccessibleToilet)
                AND (:hasDiaperTable IS NULL OR t.has_diaper_table = :hasDiaperTable)
                AND (
                  :isAvailable IS NULL
                  OR (
                        CASE
                          WHEN t.open_24h = 1 THEN 1
                          WHEN t.open_time IS NULL OR t.close_time IS NULL THEN 0
                          WHEN t.open_time < t.close_time
                               THEN (TIME(:now) BETWEEN t.open_time AND t.close_time)
                          ELSE
                               (TIME(:now) >= t.open_time OR TIME(:now) < t.close_time)
                        END
                     ) = :isAvailable
                )
              """,
            nativeQuery = true
    )
    Page<ToiletWithDistance> searchByRating(
            @Param("keyword") String keyword,
            @Param("minRating") Double minRating,
            @Param("type") String type,
            @Param("genderType") String genderType,
            @Param("isOpen24h") Boolean isOpen24h,
            @Param("hasIndoorToilet") Boolean hasIndoorToilet,
            @Param("hasBidet") Boolean hasBidet,
            @Param("hasAccessibleToilet") Boolean hasAccessibleToilet,
            @Param("hasDiaperTable") Boolean hasDiaperTable,
            @Param("providesSanitaryItems") Boolean providesSanitaryItems,
            @Param("isAvailable") Boolean isAvailable,
            @Param("now") String nowTime,
            Pageable pageable
    );
}