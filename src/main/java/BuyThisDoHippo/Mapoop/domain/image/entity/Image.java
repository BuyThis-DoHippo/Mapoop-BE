package BuyThisDoHippo.Mapoop.domain.image.entity;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
public class Image extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // S3에 저장된 이미지 URL
    @Column(nullable = false)
    private String imageUrl;

    /** TOILET or REVIEW */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    /** 사진이 등록된 리뷰 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    /** 사진이 등록된 화장실 정보 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toilet_id")
    private Toilet toilet;
}
