package BuyThisDoHippo.Mapoop.domain.image.service;

import BuyThisDoHippo.Mapoop.domain.image.dto.ImageSavedDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class S3ImageServiceImpl implements S3ImageService {

    private final S3Client s3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    // CloudFront 쓰면 도메인으로 교체: https://dxxxxx.cloudfront.net/%s
    @Value("${aws.public-url-format:https://%s.s3.%s.amazonaws.com/%s}")
    private String publicUrlFormat;

    // 허용 확장자/사이즈
    private static final Set<String> ALLOWED_EXTS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB


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
//                    .acl(ObjectCannedACL.PUBLIC_READ) // 공개 URL로 보려면 필요
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
    public List<String> uploadToiletImages(Long toiletId, List<MultipartFile> images) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : images) {
            // 검증(확장자/사이즈)
            String extWithDot = getExt(f.getOriginalFilename()); // ".jpg"
            String pureExt = extWithDot.substring(1).toLowerCase(); // "jpg"
            if (!ALLOWED_EXTS.contains(pureExt)) {
                throw new IllegalArgumentException("허용되지 않은 확장자: " + pureExt);
            }
            if (f.getSize() <= 0 || f.getSize() > MAX_SIZE_BYTES) {
                throw new IllegalArgumentException("파일 크기 초과(최대 " + (MAX_SIZE_BYTES / (1024 * 1024)) + "MB)");
            }

            // 키 규칙: toilets/{toiletId}/{uuid}.{ext}
            String key = "toilets/" + toiletId + "/" + UUID.randomUUID() + extWithDot;

            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(f.getContentType())
//                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .contentDisposition("inline")
                    .build();

            try {
                s3.putObject(req, RequestBody.fromBytes(f.getBytes()));
            } catch (Exception e) {
                log.error("S3 업로드 실패 key={}", key, e);
                throw new RuntimeException("S3 업로드 실패: " + key, e);
            }

            urls.add(String.format(publicUrlFormat, bucket, region, key));
        }
        return urls;
    }

    @Override
    public List<ImageSavedDto> uploadToiletImagesWithMeta(Long toiletId, List<MultipartFile> images) {
        List<ImageSavedDto> result = new ArrayList<>();
        for (MultipartFile f : images) {
            String originalName = (f.getOriginalFilename() == null) ? "unknown" : f.getOriginalFilename();
            String extWithDot = getExt(originalName);          // ".jpg"
            String pureExt = extWithDot.substring(1).toLowerCase(); // "jpg"
            if (!ALLOWED_EXTS.contains(pureExt)) {
                throw new IllegalArgumentException("허용되지 않은 확장자: " + pureExt);
            }
            if (f.getSize() <= 0 || f.getSize() > MAX_SIZE_BYTES) {
                throw new IllegalArgumentException("파일 크기 초과(최대 " + (MAX_SIZE_BYTES / (1024 * 1024)) + "MB)");
            }

            String key = "toilets/" + toiletId + "/" + UUID.randomUUID() + extWithDot;

            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(f.getContentType())
//                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .contentDisposition("inline")
                    .build();

            try {
                s3.putObject(req, RequestBody.fromBytes(f.getBytes()));
            } catch (Exception e) {
                log.error("S3 업로드 실패 key={}", key, e);
                throw new RuntimeException("S3 업로드 실패: " + key, e);
            }

            String url = String.format(publicUrlFormat, bucket, region, key);
            result.add(new ImageSavedDto(url, key, originalName, f.getSize(), f.getContentType()));
        }
        return result;
    }

    @Override
    public String toPublicUrl(String s3Key) {
        return "";
    }

    /**
     * 안전한 URL → key 역추출
     * - https://<bucket>.s3.<region>.amazonaws.com/<key> -> <key>
     * - https://s3.<region>.amazonaws.com/<bucket>/<key> -> <key>
     * - https://cdn.example.com/<key> -> <key>
     */
    @Override
    public String extractS3KeyFromUrl(String imageUrl) {
        URI uri = URI.create(imageUrl);
        String path = uri.getPath(); // "/<something>"

        if (path == null || path.length() <= 1) {
            throw new IllegalArgumentException("유효하지 않은 이미지 URL: " + imageUrl);
        }
        String key = path.substring(1); // 선행 "/" 제거

        // path-style S3: "/<bucket>/<key>" → "<key>" 로 정정
        if (key.startsWith(bucket + "/")) {
            key = key.substring(bucket.length() + 1);
        }

        // ⭐⭐⭐ 이 로그가 핵심!!! ⭐⭐⭐
        log.info("extractS3KeyFromUrl: Original URL = [{}], Extracted Key = [{}]", imageUrl, key);
        // ⭐⭐⭐ 그리고 여기서 추출된 'key' 값을 네 S3 콘솔에서 직접 찾아봐! ⭐⭐⭐
        return key;
    }

    private String getExt(String name) {
        if (name == null) return ".jpg";
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i) : ".jpg";
    }


}
