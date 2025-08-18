package BuyThisDoHippo.Mapoop.domain.toilet.service;

import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterRequest;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterResponse;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.domain.user.repository.UserRepository;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class ToiletService {
    private final ToiletRepository toiletRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    private final TagService tagService;

    public ToiletRegisterResponse createToilet(ToiletRegisterRequest request, Long userId) {
        log.debug("화장실 등록 요청 - 등록자 id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.USER_NOT_FOUND));

        ToiletType toiletType = ToiletType.fromString(request.getType())
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.INVALID_TOILET_TYPE));


        final boolean isOpen24h = Boolean.TRUE.equals(request.getIsOpen24h());
        final LocalTime openTime = isOpen24h ? null : request.getOpenTime();
        final LocalTime closeTime = isOpen24h ? null : request.getCloseTime();

        // 좌표 변환
        GeocodingService.GeoLocation geoLocation = geocodingService.getGeoLocation(request.getAddress());
        if (geoLocation == null || geoLocation.getLat() == null || geoLocation.getLon() == null) {
            throw new ApplicationException(CustomErrorCode.GEOCODING_FAILED);
        }

        Toilet newToilet = Toilet.builder()
                .name(request.getName().trim())
                .type(toiletType)
                .isPartnership(false)
                .latitude(geoLocation.getLat())
                .longitude(geoLocation.getLon())
                .address(geoLocation.getAddress() != null ? geoLocation.getAddress() : request.getAddress())
                .floor(request.getFloor())
                .openTime(openTime)
                .closeTime(closeTime)
                .open24h(isOpen24h)
                .avgRating(0.0)
                .totalReviews(0)
                .description(request.getDescription())
                .particulars(request.getParticulars())
                .user(user)
                .build();

        Toilet saved = toiletRepository.save(newToilet);
        /// TODO: Image 함께 요청됐다면 연결

        // 함께 요청된 태그 연결
        tagService.attachByNames(saved, request.getTags());

        log.debug("화장실 등록 완료 - id: {}, name: {}", newToilet.getId(), newToilet.getName());
        return ToiletRegisterResponse.builder()
                .id(newToilet.getId())
                .name(newToilet.getName())
                .type(newToilet.getType().name())
                .build();
    }
}
