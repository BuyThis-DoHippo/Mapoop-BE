package BuyThisDoHippo.Mapoop.domain.review.entity;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer rating;

    /** ACTIVE, HIDDEN, DELETED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewType type;

    /** 리뷰 작성자 (N:1)*/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 해당 리뷰가 달린 화장실 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toilet_id", nullable = false)
    private Toilet toilet;

    @Builder
    public Review(User user, Toilet toilet, Integer rating, String title, String content) {
        this.user = user;
        this.toilet = toilet;
        this.rating = rating;
        this.title = title;
        this.content = content;
        this.type = ReviewType.ACTIVE;
    }

    // 리뷰 수정
    public void updateReview(Integer rating, String title, String content) {
        if (rating != null) this.rating = rating;
        if (title != null) this.title = title;
        if (content != null) this.content = content;
    }

    // 리뷰 삭제 (소프트 삭제)
    public void deleteReview() {
        this.type = ReviewType.DELETED;
    }

    // 작성자 확인
    public boolean isWrittenBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    /**
     * 리뷰 이미지 URL 목록 조회
     * 추후 Image 엔티티와 연관관계 설정 후 구현
     * 현재는 임시로 빈 리스트 반환
     */
    public java.util.List<String> getImageUrls() {
        // TODO: Image 엔티티와 연관관계 설정 후 실제 이미지 URL 조회
        return java.util.Collections.emptyList();
    }

}
