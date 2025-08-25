package BuyThisDoHippo.Mapoop.domain.image.entity;

import BuyThisDoHippo.Mapoop.domain.review.entity.Review;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "image")
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // S3에 저장된 이미지 URL
    @Column(nullable = false, length = 500)
    private String imageUrl;

    // === S3 관련 추가 필드들 ===

    /** 원본 파일명 */
    @Column(nullable = false, length = 255)
    private String originalName;

    /** 파일 크기 (bytes) */
    @Column(nullable = false)
    private Long fileSize;

    /** MIME 타입 (image/jpeg, image/png 등) */
    @Column(nullable = false, length = 50)
    private String mimeType;

    /** S3 객체 키 (삭제시 필요) */
    @Column(nullable = false, length = 255)
    private String s3Key;

    /** 이미지 너비 (선택사항) */
    @Column
    private Integer width;

    /** 이미지 높이 (선택사항) */
    @Column
    private Integer height;

    // === 기존 관계 매핑 ===

    /** 사진이 등록된 리뷰 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    /** 사진이 등록된 화장실 정보 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toilet_id")
    private Toilet toilet;

    // === 정적 팩토리 메서드들 ===

    /**
     * 리뷰 이미지 생성 (width, height 없이)
     */
    public static Image createReviewImage(
            String imageUrl,
            String originalName,
            Long fileSize,
            String mimeType,
            String s3Key,
            Review review) {
        return createReviewImage(imageUrl, originalName, fileSize, mimeType, s3Key, review, null, null);
    }

    /**
     * 리뷰 이미지 생성 (전체 파라미터)
     */
    public static Image createReviewImage(
            String imageUrl,
            String originalName,
            Long fileSize,
            String mimeType,
            String s3Key,
            Review review,
            Integer width,
            Integer height) {
        return Image.builder()
                .imageUrl(imageUrl)
                .originalName(originalName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .s3Key(s3Key)
                .width(width)
                .height(height)
                .review(review)
                .build();
    }

    /**
     * 화장실 이미지 생성
     */
    public static Image createToiletImage(
            String imageUrl,
            String originalName,
            Long fileSize,
            String mimeType,
            String s3Key,
            Toilet toilet,
            Integer width,
            Integer height) {
        return Image.builder()
                .imageUrl(imageUrl)
                .originalName(originalName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .s3Key(s3Key)
                .width(width)
                .height(height)
                .toilet(toilet)
                .build();
    }

    public static Image createToiletImageWithoutToilet(
            String imageUrl,
            String originalName,
            Long fileSize,
            String mimeType,
            String s3Key,
            Integer width,
            Integer height) {
        return Image.builder()
                .imageUrl(imageUrl)
                .originalName(originalName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .s3Key(s3Key)
                .width(width)
                .height(height)
                .build();
    }

    /**
     * S3 URL에서 S3 키 추출
     */
    public String extractS3Key() {
        return this.s3Key;
    }

    /**
     * 파일 크기를 읽기 쉬운 형태로 반환
     */
    public String getReadableFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

}