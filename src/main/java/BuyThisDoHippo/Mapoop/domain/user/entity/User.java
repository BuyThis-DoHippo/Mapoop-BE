package BuyThisDoHippo.Mapoop.domain.user.entity;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(nullable = false)
    private String kakaoId;

    @Column(nullable = false)
    private Boolean isLocationConsent;

    private LocalDateTime locationConsentDate;

    // 약관 버전 필요하다면 추가

    public void updateLocationConsent(Boolean consent, String consentVersion) {
        this.isLocationConsent = consent;
        this.locationConsentDate = consent ? LocalDateTime.now() : null;
    }


}
