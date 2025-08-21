package BuyThisDoHippo.Mapoop.domain.toilet.entity;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "toilet",
        indexes = {
            @Index(name = "idx_toilet_location", columnList = "latitude, longitude"),
            @Index(name = "idx_toilet_type", columnList = "type"),
            @Index(name = "idx_toilet_rating", columnList = "avg_rating")
        })
public class Toilet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** STORE or PUBLIC */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToiletType type;

    @Column(nullable = false)
    private Boolean isPartnership;

    private String description; // 장소(가게) 설명
    private String particulars; // 화장실 특이사항

    /** 위치 관련 */
    @Column(nullable = false)
    private Double latitude;    // 위도
    @Column(nullable = false)
    private Double longitude;   // 경도
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private Integer floor;

    /** 평점 관련 */
    @Column(name = "avg_rating")
    private Double avgRating;
    private Integer totalReviews;

    /** 운영 정보 */
    @Column(name = "open_24h", nullable = false)
    private Boolean open24h;
    @Column(name = "open_time")
    private LocalTime openTime;
    @Column(name = "close_time")
    private LocalTime closeTime;

    /** 등록한 유저 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 태그 조회용 (1:N) */
    @OneToMany(mappedBy = "toilet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToiletTag> toiletTags = new ArrayList<>();

    public boolean isOpenNow(LocalTime now) {
        if (open24h) return true;
        if (openTime == null || closeTime == null) return false;
        if (openTime.equals(closeTime)) return false;
        if (openTime.isBefore(closeTime)) {
            return !now.isBefore(openTime) && now.isBefore(closeTime);
        } else {
            // 야간 영업
            return !now.isBefore(openTime) || now.isBefore(closeTime);
        }
    }

}
