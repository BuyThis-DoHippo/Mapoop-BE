package BuyThisDoHippo.Mapoop.domain.image.entity;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Table(name = "review_image",
        indexes = {
                @Index(name = "idx_review_image_review", columnList = "review_id")
        })
public class ReviewImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 리뷰의 이미지인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /** S3 퍼블릭 URL */
    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;
}
