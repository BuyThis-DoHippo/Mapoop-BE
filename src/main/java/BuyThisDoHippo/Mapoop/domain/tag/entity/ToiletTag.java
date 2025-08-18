package BuyThisDoHippo.Mapoop.domain.tag.entity;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "toilet_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"toilet_id", "tag_id"}),
        indexes = {
        @Index(name = "idx_toilet_tag_tag_toilet", columnList = "tag_id, toilet_id"),   // 태그로 화장실 찾기 가속
        @Index(name = "idx_toilet_tag_toilet", columnList = "toilet_id")    // 화장실 상세에서 태그 나열 가속
            }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToiletTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 달린 태그 정보 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    /** 달린 화장실 정보 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "toilet_id", nullable = false)
    private Toilet toilet;

    public static ToiletTag link(Toilet toilet, Tag tag) {
        return ToiletTag.builder()
                .toilet(toilet)
                .tag(tag)
                .build();
    }
}
