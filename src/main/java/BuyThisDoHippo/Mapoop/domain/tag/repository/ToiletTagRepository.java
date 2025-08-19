package BuyThisDoHippo.Mapoop.domain.tag.repository;

import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ToiletTagRepository extends JpaRepository<ToiletTag, Long> {
    List<ToiletTag> findByToiletId(Long toiletId);

    @Query("""
        select tt.tag.id
        from ToiletTag tt
        where tt.toilet.id = :toiletId and tt.tag.id in :tagIds
    """)
    Set<Long> findAttachedTagIds(@Param("toiletId") Long toiletId, @Param("tagIds") Collection<Long> tagIds);

    @Query("""
        select tt.tag.id
        from ToiletTag tt
        where tt.toilet.id = :toiletId
    """)
    Set<Long> findTagIdsByToiletId(@Param("toiletId") Long toiletId);

    @Modifying
    @Query("""
        delete from ToiletTag tt
        where tt.toilet.id = :toiletId and tt.tag.id in :removeTagIds
    """)
    void deleteByToiletIdAndTagIdIn(@Param("toiletId") Long toiletId,
                                   @Param("removeTagIds") Collection<Long> removeTagIds);


    // 인터페이스 기반 Projection
    interface ToiletIdTagName {
        Long getToiletId();
        String getTagName();
    }

    // 다건 (id-name 연결해서 응답)
    @Query("""
        select tt.toilet.id as toiletId, tt.tag.name as tagName
        from ToiletTag tt
        where tt.toilet.id in :toiletIds
    """)
    List<ToiletIdTagName> findTagNamesByToiletIds(@Param("toiletIds") Collection<Long> toiletIds);

    // 단건 (name만 필요함)
    @Query("""
    select tg.name
    from ToiletTag tt
    join tt.tag tg
    where tt.toilet.id = :toiletId
""")
    List<String> findTagNamesByToiletId(@Param("toiletId") Long toiletId);
}
