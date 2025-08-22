package BuyThisDoHippo.Mapoop.domain.image.service;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.image.repository.ImageRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
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

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Long saveToiletImage(Long toiletId, String imageUrl, String s3Key,
                                String originalName, long size, String contentType) {
        Toilet toiletRef = em.getReference(Toilet.class, toiletId);
        Image image = Image.createToiletImage(imageUrl, originalName, size, contentType, s3Key, toiletRef, null, null);
        return imageRepository.save(image).getId();
    }

    public void deleteImage(Long toiletId, Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.IMAGE_NOT_FOUND));

        if (image.getToilet() == null || !Objects.equals(image.getToilet().getId(), toiletId)) {
            throw new ApplicationException(CustomErrorCode.INVALID_REQUEST_DTO);
        }
        s3ImageService.deleteImage(image.getImageUrl());
        imageRepository.delete(image);
    }

    public void deleteAllImages(Long toiletId) {
        List<Image> images = imageRepository.findByToilet_IdOrderByCreatedAtAsc(toiletId);
        if (images.isEmpty()) return;

        for (Image image : images) {
            s3ImageService.deleteImage(image.getImageUrl());
            imageRepository.delete(image);
        }

    }
}
