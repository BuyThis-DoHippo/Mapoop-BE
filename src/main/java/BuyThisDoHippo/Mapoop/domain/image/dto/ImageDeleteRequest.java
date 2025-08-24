package BuyThisDoHippo.Mapoop.domain.image.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageDeleteRequest {
    private List<String> imageUrls; // 삭제할 이미지 URL 목록
}
