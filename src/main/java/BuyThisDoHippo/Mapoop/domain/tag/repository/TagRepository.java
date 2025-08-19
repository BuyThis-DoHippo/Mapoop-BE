package BuyThisDoHippo.Mapoop.domain.tag.repository;

import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findAllByNameIn(Collection<String> names);

    @Query("select t.id from Tag t where t.name in :names")
    List<Long> findIdsByNames(@Param("names") List<String> names);
}
