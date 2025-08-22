package BuyThisDoHippo.Mapoop.domain.image.service;

import BuyThisDoHippo.Mapoop.domain.image.dto.ImageSavedDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(value = "aws.enabled", havingValue = "false", matchIfMissing = true)
public class NoopS3ImageService implements S3ImageService {

    @Override
    public List<String> uploadReviewImages(Long reviewId, List<MultipartFile> images) {
        log.warn("S3 비활성화: 업로드 생략");
        return new ArrayList<>();
    }

    @Override
    public String uploadSingleImage(Long reviewId, MultipartFile image) {
        log.warn("S3 비활성화: 업로드 생략");
        return "";
    }

    @Override public void deleteImage(String imageUrl) { }
    @Override public void deleteReviewImages(List<String> imageUrls) { }

    @Override
    public List<String> uploadToiletImages(Long toiletId, List<MultipartFile> images) {
        return List.of();
    }

    @Override
    public List<ImageSavedDto> uploadToiletImagesWithMeta(Long toiletId, List<MultipartFile> images) {
        return List.of();
    }

    @Override
    public String toPublicUrl(String s3Key) {
        return "";
    }

    @Override
    public String extractS3KeyFromUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("/"))
            return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        return "default-key";
    }
}
