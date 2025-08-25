package BuyThisDoHippo.Mapoop.domain.search.dto;

import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionDto {
    private Long toiletId;
    private String name;
    private Double rating;

    public static SearchSuggestionDto from(Toilet toilet) {
        return SearchSuggestionDto.builder()
                .toiletId(toilet.getId())
                .name(toilet.getName())
                .rating(toilet.getAvgRating())
                .build();
    }
}
