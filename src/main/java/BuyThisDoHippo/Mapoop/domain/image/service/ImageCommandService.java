package BuyThisDoHippo.Mapoop.domain.image.service;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.image.repository.ImageRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCommandService {
    private final ImageRepository imageRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Long saveToiletImage(Long toiletId, String imageUrl, String s3Key,
                                String originalName, long size, String contentType) {
        Toilet toiletRef = em.getReference(Toilet.class, toiletId);
        Image image = Image.createToiletImage(imageUrl, originalName, size, contentType, s3Key, toiletRef, null, null);
        return imageRepository.save(image).getId();
    }
}
