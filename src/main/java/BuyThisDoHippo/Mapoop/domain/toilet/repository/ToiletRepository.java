package BuyThisDoHippo.Mapoop.domain.toilet.repository;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToiletRepository extends JpaRepository<Toilet, Long> {
    List<Toilet> findByNameContainingIgnoreCaseOrderByAvgRatingDesc(String keyword, Pageable pageable);
}
