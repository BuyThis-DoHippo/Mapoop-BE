package BuyThisDoHippo.Mapoop.domain.image.service;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.image.repository.ImageRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletImage;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletImageRepository;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCommandService {
    private final ImageRepository imageRepository;
    private final S3ImageService s3ImageService;
    private final ToiletImageRepository toiletImageRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Long saveToiletImage(Long toiletId, String imageUrl, String s3Key,
                                String originalName, long size, String contentType) {
        Toilet toiletRef = em.getReference(Toilet.class, toiletId);
        Image image = Image.createToiletImage(imageUrl, originalName, size, contentType, s3Key, toiletRef, null, null);
        return imageRepository.save(image).getId();
    }

    public void deleteImage(Long imageId) {
        ToiletImage toiletImage = toiletImageRepository.findByImageIdWithImage(imageId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.IMAGE_NOT_FOUND));

        Image image = toiletImage.getImage();
        String imageUrl = image.getImageUrl();

        toiletImageRepository.delete(toiletImage);
        toiletImageRepository.flush();

        boolean referencedElsewhere =
                toiletImageRepository.existsByImage(image);

        if (!referencedElsewhere) {
            s3ImageService.deleteImage(imageUrl);  // S3 삭제
            imageRepository.delete(image);         // DB 이미지 삭제
        }
    }

    public void deleteAllImages(Long toiletId) {
        List<ToiletImage> links = toiletImageRepository.findAllByToiletIdWithImage(toiletId);
        if (links.isEmpty()) return;

        List<Image> images = links.stream().map(ToiletImage::getImage).toList();
        List<String> urls  = images.stream().map(Image::getImageUrl).toList();

        toiletImageRepository.deleteAllInBatch(links);
        toiletImageRepository.flush();

        // S3 → DB 순서로 삭제
        for (String url : urls) {
            s3ImageService.deleteImage(url);
        }
        imageRepository.deleteAllInBatch(images);

    }

    public void attachByIds(Toilet toilet, List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;

        // 1. id들로 이미지 엔티티 리스트 찾기
        List<Image> images = imageRepository.findAllById(imageIds);
        if (images.isEmpty()) return;

        // 2. ToiletImage 엔티티 생성 (toilet_image 조인 테이블용)
        List<ToiletImage> toiletImages = images.stream()
                .map(image -> ToiletImage.builder()
                        .toilet(toilet)
                        .image(image)
                        .build())
                .toList();

        // 3. 저장
        toiletImageRepository.saveAll(toiletImages);
    }

    @Transactional
    public Long saveToiletImage(String imageUrl, String s3Key,
                                String originalName, long size, String contentType) {
        Image image = Image.createToiletImageWithoutToilet(imageUrl, originalName, size, contentType, s3Key, null, null);
        return imageRepository.save(image).getId();
    }

    @Transactional
    public void attachOnlyNew(Toilet toilet, List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;

        // 이미 연결된 이미지 id 집합
        var existing = new java.util.HashSet<>(toiletImageRepository.findImageIdsByToiletId(toilet.getId()));

        // 신규만 추림
        List<Long> toAttach = imageIds.stream()
                .filter(id -> !existing.contains(id))
                .toList();

        if (toAttach.isEmpty()) return;

        // 기존 attach 로직 재활용
        attachByIds(toilet, toAttach);
    }
}
