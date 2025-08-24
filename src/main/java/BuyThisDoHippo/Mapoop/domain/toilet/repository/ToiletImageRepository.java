package BuyThisDoHippo.Mapoop.domain.toilet.repository;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToiletImageRepository extends JpaRepository<ToiletImage, Long> {
    List<ToiletImage> findByToiletId(Long toiletId);

    Optional<ToiletImage> findByImageId(Long imageId);
}
