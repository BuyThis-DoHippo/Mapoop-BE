package BuyThisDoHippo.Mapoop.domain.tag.repository;

import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToiletTagRepository extends JpaRepository<ToiletTag, Long> {
    List<ToiletTag> findByToiletId(Long toiletId);
}
