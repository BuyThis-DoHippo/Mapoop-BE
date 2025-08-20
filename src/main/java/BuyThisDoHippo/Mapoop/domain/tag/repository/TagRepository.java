package BuyThisDoHippo.Mapoop.domain.tag.repository;

import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    List<Tag> findAllByOrderByName();

    @Query("""
        SELECT t FROM Tag t 
        WHERE t.name IN (
            '현재이용가능', '남녀분리', '가까이안화장실', '24시간', 
            '비데있음', '위생용품제공', '깨끗함', '간판옴', 
            '장애인화장실', '기저귀교환대'
        )
        ORDER BY t.name
        """)
    List<Tag> findReviewTags();

    List<Tag> findByIdIn(List<Long> tagIds);
}
