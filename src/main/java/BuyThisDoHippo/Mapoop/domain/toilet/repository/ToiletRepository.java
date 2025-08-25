package BuyThisDoHippo.Mapoop.domain.toilet.repository;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ToiletRepository extends JpaRepository<Toilet, Long> {

    List<Toilet> findByUserId(Long userId);

    List<Toilet> findByNameContainingIgnoreCaseOrderByAvgRatingDesc(String keyword, Pageable pageable);

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