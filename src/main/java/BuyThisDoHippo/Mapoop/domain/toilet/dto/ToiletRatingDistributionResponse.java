package BuyThisDoHippo.Mapoop.domain.toilet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToiletRatingDistributionResponse {
    private Long totalReviews;
    private List<RatingDistribution> distribution;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private Integer rating;
        private Long count;
        private Double percentage;
    }
}
