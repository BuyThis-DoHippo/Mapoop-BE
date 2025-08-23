package BuyThisDoHippo.Mapoop.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 화장실 평점 분포 응답 DTO
 * 1~5점별 리뷰 개수와 비율을 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDistributionResponse {
    private Integer totalReviews;
    private List<RatingDistribution> distribution;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private Integer rating;
        private Integer count;
        private Double percentage;
    }
}
