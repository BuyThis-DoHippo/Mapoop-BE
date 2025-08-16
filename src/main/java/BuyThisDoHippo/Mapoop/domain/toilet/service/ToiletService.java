package BuyThisDoHippo.Mapoop.domain.toilet.service;

import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterRequest;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterResponse;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.GenderType;
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

        GenderType genderType = GenderType.fromString(request.getGenderType())
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.INVALID_GENDER_TYPE));

        final boolean isOpen24h = Boolean.TRUE.equals(request.getOpen24h());
        final LocalTime openTime = isOpen24h ? null : request.getOpenTime();
        final LocalTime closeTime = isOpen24h ? null : request.getCloseTime();

        // 좌표 변환
        GeocodingService.GeoLocation geoLocation = geocodingService.getGeoLocation(request.getAddress());

        Toilet newToilet = Toilet.builder()
                .name(request.getName().trim())
                .type(toiletType)
                .genderType(genderType)
                .isPartnership(false)
                .latitude(geoLocation.getLat())
                .longitude(geoLocation.getLon())
                .address(geoLocation.getAddress())  // formatted address 저장
                .floor(request.getFloor())
                .avgRating(null)
                .totalReviews(0)
                .description(request.getDescription())
                .particulars(request.getParticulars())
                .open24h(request.getOpen24h())
                .openTime(openTime)
                .closeTime(closeTime)
                .hasDiaperTable(request.getHasDiaperTable())
                .hasIndoorToilet(request.getHasIndoorToilet())
                .hasBidet(request.getHasBidet())
                .providesSanitaryItems(request.getProvidesSanitaryItems())
                .hasAccessibleToilet(request.getHasAccessibleToilet())
                .user(user)
                .build();

        Long toiletId = toiletRepository.save(newToilet).getId();
        log.debug("화장실 등록 완료 - id: {}, name: {}", newToilet.getId(), newToilet.getName());

        // Tag 정보 있다면 연결
        tagService.attachTags(toiletId, request.getTags());
        /// TODO: Image 함께 요청됐다면 연결

        return ToiletRegisterResponse.builder()
                .id(newToilet.getId())
                .name(newToilet.getName())
                .type(newToilet.getType().name())
                .build();
    }
}
