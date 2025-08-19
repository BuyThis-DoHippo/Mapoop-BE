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
    

    @Query("""
      select distinct t
      from Toilet t
      left join fetch t.toiletTags tt        
      left join fetch tt.tag tag         
      where (:keyword is null
             or lower(t.name) like lower(concat('%', :keyword, '%'))
             or lower(t.address) like lower(concat('%', :keyword, '%')))
        and (:minRating is null or t.avgRating >= :minRating)
        and (:type is null or t.type = :type)
        and (
             :tagIds is null
             or t.id in (
                 select tt2.toilet.id
                 from ToiletTag tt2
                 where tt2.tag.id in :tagIds
                 group by tt2.toilet.id
                 having count(distinct tt2.tag.id) = :tagCnt
             )
        )
        and (
             :requireAvailable = false
             or (
                  t.open24h = true
                  or (
                      t.openTime is not null and t.closeTime is not null and
                      (
                          (t.openTime <  t.closeTime and :now between t.openTime and t.closeTime)
                          or
                          (t.openTime >= t.closeTime and (:now >= t.openTime or :now < t.closeTime))
                      )
                  )
             )
        )
    """)
    List<Toilet> searchAllFiltered(@Param("keyword") String keyword,
                                   @Param("minRating") Double minRating,
                                   @Param("type") ToiletType type,
                                   @Param("tagIds") List<Long> tagIds,
                                   @Param("tagCnt") long tagCnt,
                                   @Param("requireAvailable") boolean requireAvailable,
                                   @Param("now") LocalTime now);


}