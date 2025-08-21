package BuyThisDoHippo.Mapoop.domain.toilet.service;

import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.ToiletTagRepository;
import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.*;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class ToiletService {
    private static final String TAG_AVAILABLE_NOW = "현재이용가능";

    private final ToiletRepository toiletRepository;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    private final TagService tagService;
    private final ToiletTagRepository toiletTagRepository;

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

    @Transactional(readOnly = true)
    public ToiletDetailResponse getToiletDetail(Long toiletId, LocalTime now) {
        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.TOILET_NOT_FOUND));

        List<String> tagNames = toiletTagRepository.findTagNamesByToiletId(toiletId);
        if(toilet.isOpenNow(LocalTime.now(ZoneId.of("Asia/Seoul")))) {
            if(!tagNames.contains(TAG_AVAILABLE_NOW))
                tagNames.add(TAG_AVAILABLE_NOW);
        }

        return ToiletDetailResponse.builder()
                .id(toilet.getId())
                .name(toilet.getName())
                .type(toilet.getType().name())
                .location(ToiletDetailResponse.Location.builder()
                        .latitude(toilet.getLatitude())
                        .longitude(toilet.getLongitude())
                        .address(toilet.getAddress())
                        .floor(toilet.getFloor())
                        .build()
                )
                .rating(ToiletDetailResponse.Rating.builder()
                        .avgRating(toilet.getAvgRating())
                        .totalReviews(toilet.getTotalReviews())
                        .build())
                .hours(ToiletDetailResponse.Hours.builder()
                        .openTime(toilet.getOpenTime())
                        .closeTime(toilet.getCloseTime())
                        .isOpen24h(Boolean.TRUE.equals(toilet.getOpen24h()))
                        .isOpenNow(toilet.isOpenNow(LocalTime.now()))
                        .build())
                .isPartnership(Boolean.TRUE.equals(toilet.getIsPartnership()))
                .description(toilet.getDescription())
                .particulars(toilet.getParticulars())
                .images(List.of())               // 이미지 나중 반영
                .tags(tagNames)
                .build();
    }

    public void updateToilet(Long toiletId, Long userId, ToiletUpdateRequest request) {
        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.TOILET_NOT_FOUND));

        if (!toilet.getUser().getId().equals(userId)) {
            throw new ApplicationException(CustomErrorCode.FORBIDDEN);
        }

        toilet.setName(request.getName().trim());
        ToiletType type = ToiletType.fromString(request.getType())
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.INVALID_TOILET_TYPE));
        toilet.setType(type);

        // 좌표 관련
        boolean addressChanged = !Objects.equals(toilet.getAddress(), request.getAddress());
        toilet.setAddress(request.getAddress());
        if (addressChanged) {
            GeocodingService.GeoLocation loc = geocodingService.getGeoLocation(request.getAddress());
            toilet.setLatitude(loc.getLat());
            toilet.setLongitude(loc.getLon());
            toilet.setAddress(loc.getAddress());
        }
        toilet.setFloor(request.getFloor());

        // 운영 시간 관련
        boolean is24 = Boolean.TRUE.equals(request.getIsOpen24h());
        toilet.setOpen24h(is24);
        if (is24) {
            toilet.setOpenTime(null);
            toilet.setCloseTime(null);
        } else {
            if (request.getOpenTime() == null || request.getCloseTime() == null) {
                throw new ApplicationException(CustomErrorCode.MISSING_REQUIRED_PARAM);
            }
            toilet.setOpenTime(request.getOpenTime());
            toilet.setCloseTime(request.getCloseTime());
        }

        // 태그 관련
        if (request.getTags() != null) {
            tagService.syncTags(toilet, request.getTags());
        }

        toiletRepository.save(toilet);
        log.info("화장실 정보 수정 완료 - toilet id: {}, updated by id: {}", toiletId, userId);
    }

    public List<ToiletSimpleInfo> getToiletsByUserId(Long userId) {
        List<Toilet> toilets = toiletRepository.findByUserId(userId);

        return toilets.stream()
                .map(toilet -> ToiletSimpleInfo.builder()
                        .id(toilet.getId())
                        .name(toilet.getName())
                        .type(toilet.getType().name())
                        .address(toilet.getAddress())
                        .floor(toilet.getFloor())
                        .createdAt(toilet.getCreatedAt().toLocalDate())
                        .build())
                .toList();
    }
}
