package BuyThisDoHippo.Mapoop.domain.tag.repository;

import BuyThisDoHippo.Mapoop.domain.tag.entity.ReviewTag;
import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewTagRepository extends JpaRepository<ReviewTag, Long> {

    List<ReviewTag> findByReviewId(Long reviewId);

    void deleteByReviewId(Long reviewId);

    @Query("""
        SELECT t.name, COUNT(rt) as tagCount
        FROM ReviewTag rt
        JOIN rt.tag t
        JOIN rt.review r
        WHERE r.toilet.id = :toiletId
        AND r.type = 'ACTIVE'
        GROUP BY t.id, t.name
        ORDER BY tagCount DESC 
        """)
    List<Object[]> findTopTagsByToiletId(@Param("toiletId") Long toiletId);

    @Query("""
        SELECT t.name
        FROM ReviewTag rt
        JOIN rt.tag t
        JOIN rt.review r
        WHERE r.toilet.id = :toiletId
        AND r.type = 'ACTIVE'
        GROUP BY t.id, t.name
        ORDER BY COUNT(rt) DESC 
        LIMIT 3
        """)
    List<Tag> findTop3TagsByToiletId(@Param("toiletId") Long toiletId);

    @Query("""
        SELECT rt
        FROM ReviewTag rt
        JOIN FETCH rt.tag
        WHERE rt.review.id IN :reviewIds
        """)
    List<ReviewTag> findTagsByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    Long countByTagId(Long tagId);

    @Query("""
        SELECT COUNT(rt)
        FROM ReviewTag rt
        JOIN rt.review r
        WHERE r.toilet.id = :toiletId
        AND rt.tag.id = :tagId
        AND r.type = 'ACTIVE'
        """)
    Long countByToiletIdAndTagId(@Param("toiletId") Long toiletId, @Param("tagId") Long tagId);

}
