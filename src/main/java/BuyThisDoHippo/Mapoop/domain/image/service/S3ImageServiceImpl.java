package BuyThisDoHippo.Mapoop.domain.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class S3ImageServiceImpl implements S3ImageService {

    private final S3Client s3;

    @Value("${cloud.aws.s3.bucket}") private String bucket;
    @Value("${cloud.aws.region.static}")    private String region;
    // CloudFront 쓰면 도메인으로 교체: https://dxxxxx.cloudfront.net/%s
    @Value("${aws.public-url-format:https://%s.s3.%s.amazonaws.com/%s}")
    private String publicUrlFormat;

    @Override
    public List<String> uploadReviewImages(Long reviewId, List<MultipartFile> images) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : images) {
            String ext = getExt(f.getOriginalFilename());
            String key = "reviews/" + reviewId + "/" + UUID.randomUUID() + ext;

            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(f.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ) // 공개 URL로 보려면 필요
                    .build();

            try {
                s3.putObject(req, RequestBody.fromBytes(f.getBytes()));
            } catch (Exception e) {
                throw new RuntimeException("S3 업로드 실패: " + key, e);
            }
            urls.add(String.format(publicUrlFormat, bucket, region, key));
        }
        return urls;
    }

    @Override
    public String uploadSingleImage(Long reviewId, MultipartFile image) {
        return uploadReviewImages(reviewId, List.of(image)).get(0);
    }

    @Override
    public void deleteImage(String imageUrl) {
        String key = extractS3KeyFromUrl(imageUrl);
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public void deleteReviewImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        DeleteObjectsRequest req = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder()
                        .objects(imageUrls.stream()
                                .map(this::extractS3KeyFromUrl)
                                .map(k -> ObjectIdentifier.builder().key(k).build())
                                .toList())
                        .build())
                .build();
        s3.deleteObjects(req);
    }

    @Override
    public String extractS3KeyFromUrl(String imageUrl) {
        int p = imageUrl.indexOf(".amazonaws.com/");
        if (p > 0) return imageUrl.substring(p + ".amazonaws.com/".length() + imageUrl.indexOf("s3.", 0) - 2);
        // CloudFront 등 커스텀 도메인인 경우:
        int slash = imageUrl.indexOf('/', 8);
        return imageUrl.substring(slash + 1);
    }

    private String getExt(String name) {
        if (name == null) return ".jpg";
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i) : ".jpg";
    }
}
