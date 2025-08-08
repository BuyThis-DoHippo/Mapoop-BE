package BuyThisDoHippo.Mapoop.domain.toilet.entity;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(nullable = false)
    private Double latitude;    // 위도

    @Column(nullable = false)
    private Double longitude;   // 경도

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Integer floor;

    private Double avgRating;   // 1 ~ 5
    private Integer totalReviews;
    private LocalTime openTime;
    private LocalTime closeTime;

    /** 등록한 유저 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 태그 조회용 (1:N) */
    @OneToMany(mappedBy = "toilet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToiletTag> tags = new ArrayList<>();

}
