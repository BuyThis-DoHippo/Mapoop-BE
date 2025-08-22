package BuyThisDoHippo.Mapoop.domain.image.dto;

import lombok.*;

@Getter
@AllArgsConstructor
public class ImageSavedDto {
    private final String url;
    private final String s3Key;
    private final String originalName;
    private final long size;    // 바이트
    private final String contentType;   // MIME
}
