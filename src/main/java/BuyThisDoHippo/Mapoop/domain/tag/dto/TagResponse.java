package BuyThisDoHippo.Mapoop.domain.tag.dto;

import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagResponse {

    private Long tagId;

    private String tagName;

    public static TagResponse from(Tag tag) {
        return TagResponse.builder()
                .tagId(tag.getId())
                .tagName(tag.getName())
                .build();
    }
}
