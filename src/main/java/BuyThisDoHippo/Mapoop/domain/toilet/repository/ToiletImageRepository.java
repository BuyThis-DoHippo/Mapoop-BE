package BuyThisDoHippo.Mapoop.domain.toilet.repository;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletImage;
import aj.org.objectweb.asm.commons.Remapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToiletImageRepository extends JpaRepository<ToiletImage, Long> {
    List<ToiletImage> findByToiletId(Long toiletId);

    Optional<ToiletImage> findByImageId(Long imageId);

    @Query("select ti.image.id from ToiletImage ti where ti.toilet.id = :toiletId")
    List<Long> findImageIdsByToiletId(@Param("toiletId") Long toiletId);

    Optional<ToiletImage> findFirstByToilet_IdOrderByCreatedAtAsc(Long id);
}
