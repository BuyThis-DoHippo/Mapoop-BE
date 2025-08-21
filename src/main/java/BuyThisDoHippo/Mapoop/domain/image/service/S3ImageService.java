package BuyThisDoHippo.Mapoop.domain.image.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface S3ImageService {
    List<String> uploadReviewImages(Long reviewId, List<MultipartFile> images);
    String uploadSingleImage(Long reviewId, MultipartFile image);
    void deleteImage(String imageUrl);
    void deleteReviewImages(List<String> imageUrls);
    String extractS3KeyFromUrl(String imageUrl);
}