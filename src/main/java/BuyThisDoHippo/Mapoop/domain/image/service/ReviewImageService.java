// ReviewImageService.java (이전에 보여줬던 코드 그대로)
package BuyThisDoHippo.Mapoop.domain.image.service; // 너의 서비스 패키지 경로에 맞게 수정해

import BuyThisDoHippo.Mapoop.domain.image.service.S3ImageService; // 너의 S3Uploader 패키지 경로에 맞게 수정해
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewImageService {

    private final S3ImageService s3ImageService;// S3에 파일 올리는 역할만 함

    // ⭐⭐ 리뷰 이미지 개별 업로드 메소드 (컨트롤러와 연결될 것) ⭐⭐
    @Transactional // S3 업로드 자체는 트랜잭션 필요 없지만, 비즈니스 로직을 포함할 수 있으니 붙여둠.
    public List<String> uploadReviewImages(Long userId, Long toiletId, List<MultipartFile> images) { // 컨트롤러에서 userId, toiletId 받아서 넘겨줄거야.
        if (images == null || images.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        // 이미지 개수 제한은 컨트롤러나 DTO validation에서 해도 좋고, 여기서 한 번 더 체크 가능.
        if (images.size() > 5) { // 예시: 최대 5개로 제한
            throw new IllegalArgumentException("한 번에 최대 5개의 이미지만 업로드할 수 있습니다.");
        }

        // ⭐⭐⭐ 여기서 S3ImageService의 uploadReviewImages 메소드를 호출! ⭐⭐⭐
        // reviewId는 지금 없지만, S3ImageService 인터페이스에 reviewId가 있어서 일단 넘겨줌.
        // 실제 S3ImageService 구현체에서 reviewId를 어떻게 처리하는지 확인 필요.
        // 예를 들어 reviewId를 경로로 쓰거나 로그에 남기거나 할 수 있음.
        List<String> imageUrls = s3ImageService.uploadReviewImages(toiletId, images); // 리뷰 ID가 아니므로 toiletId를 넘김 (혹은 그냥 null, 0L 넘기고 구현체 수정)

        log.info("사용자 {}의 화장실 {} 리뷰 이미지 {}개 S3 업로드 완료 (S3ImageService 이용). URL: {}",
                userId, toiletId, imageUrls.size(), imageUrls);

        return imageUrls;
    }

    // ⭐⭐ 리뷰 삭제 시 S3 이미지도 함께 삭제해야 한다면 이 메소드 추가 ⭐⭐
    // reviewImages 필드의 orphanRemoval = true로 DB에서는 알아서 삭제되겠지만, S3에 남은 파일은 이 메소드로 삭제.
    public void deleteS3ReviewImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        s3ImageService.deleteReviewImages(imageUrls); // ⭐⭐⭐ S3ImageService의 deleteReviewImages 호출 ⭐⭐⭐
        log.info("S3 리뷰 이미지 {}개 삭제 요청 완료", imageUrls.size());
    }
}
