package BuyThisDoHippo.Mapoop.domain.image.service;

import BuyThisDoHippo.Mapoop.domain.image.dto.ImageSavedDto;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface S3ImageService {
    List<String> uploadReviewImages(Long reviewId, List<MultipartFile> images);
    String uploadSingleImage(Long reviewId, MultipartFile image);
    void deleteImage(String imageUrl);
    void deleteReviewImages(List<String> imageUrls);

    /** 화장실 이미지 관련 추가 */
    // 1. 정적 퍼블릭 URL 목록 반환
    List<String> uploadToiletImages(Long toiletId, List<MultipartFile> images);
    // 2. 메타 포함 DTO 목록 반환
    List<ImageSavedDto>  uploadToiletImagesWithMeta(Long toiletId, List<MultipartFile> images);
    /** s3 Key <-> 퍼블릭 URL */
    String toPublicUrl(String s3Key);
    String extractS3KeyFromUrl(String imageUrl);
    List<ImageSavedDto>  uploadToiletImages(List<MultipartFile> images);
}