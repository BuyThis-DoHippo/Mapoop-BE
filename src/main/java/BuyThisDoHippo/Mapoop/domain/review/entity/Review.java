package BuyThisDoHippo.Mapoop.domain.review.entity;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.image.entity.ReviewImage;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /** 리뷰에 첨부된 이미지들 (1:N) */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReviewImage> reviewImages = new ArrayList<>();

    /**
     * 리뷰 이미지 URL 목록 조회
     */
    public List<String> getImageUrls() {
        return reviewImages.stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());
    }

    // ⭐⭐ 새로 추가: ReviewImage 편의 메소드 (서비스에서 이미지 추가할 때 사용) ⭐⭐
    public void addReviewImage(ReviewImage reviewImage) {
        this.reviewImages.add(reviewImage);
        if (reviewImage.getReview() != this) { // 무한 루프 방지 (이미 연결되어 있다면 스킵)
            reviewImage.setReview(this);
        }
    }

    // ⭐⭐ 새로 추가: 이미지 리스트를 한 번에 설정하는 메소드 (서비스에서 DTO로 받은 URL 리스트를 연결할 때 사용) ⭐⭐
    public void setReviewImages(List<ReviewImage> reviewImages) {
        this.reviewImages.clear(); // 기존 이미지 클리어
        if (reviewImages != null) {
            reviewImages.forEach(this::addReviewImage); // 새 이미지들 추가
        }
    }


}
