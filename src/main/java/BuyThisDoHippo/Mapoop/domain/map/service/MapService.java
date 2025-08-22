package BuyThisDoHippo.Mapoop.domain.map.service;

import BuyThisDoHippo.Mapoop.domain.image.entity.Image;
import BuyThisDoHippo.Mapoop.domain.image.repository.ImageRepository;
import BuyThisDoHippo.Mapoop.domain.map.dto.MapResultResponse;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerInfo;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerFilter;
import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.TagRepository;
import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.global.common.TagConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MapService {

    private final TagRepository tagRepository;
    private final ToiletRepository toiletRepository;
    private final ImageRepository imageRepository;

    @Transactional(readOnly = true)
    public MapResultResponse getMarkers(MarkerFilter filter) {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));

        // 태그 이름 → ID
        List<Long> tagIds = resolveTagIds(filter.getTags());

        List<Toilet> list = toiletRepository.searchAllFiltered(
                null,
                filter.getMinRating(),
                filter.getType(),
                tagIds.isEmpty() ? null : tagIds,
                tagIds.size(),
                filter.getRequireAvailable(),
                now
        );

        // 위치 있으면 거리순, 없으면 평점(이름)순
        Comparator<Toilet> comparator;
        if (filter.getLat() != null && filter.getLng() != null) {
            comparator = Comparator.comparingInt(
                    t -> distanceMeters(filter.getLat(), filter.getLng(), t.getLatitude(), t.getLongitude())
            );
        } else {
            comparator = Comparator
                    .<Toilet, Double>comparing(t -> Optional.ofNullable(t.getAvgRating()).orElse(0.0))
                    .reversed()
                    .thenComparing(Toilet::getName, Comparator.nullsLast(String::compareTo));
        }
        list.sort(comparator);

        // 매핑
        List<MarkerInfo> markers = list.stream().map(t -> {
            boolean available = t.isOpenNow(now);

            List<String> tagNames = t.getToiletTags().stream()
                    .map(ToiletTag::getTag)
                    .filter(Objects::nonNull)
                    .map(Tag::getName)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            if (available) tagNames.add(TagConstants.VIRTUAL_AVAILABLE);

            String mainImageUrl = imageRepository
                    .findFirstByToilet_IdOrderByCreatedAtAsc(t.getId())
                    .map(Image::getImageUrl)
                    .orElse(null);  // 기본이미지 확정되면 추후 반영

            return MarkerInfo.builder()
                    .toiletId(t.getId())
                    .type(t.getType().name())
                    .latitude(t.getLatitude())
                    .longitude(t.getLongitude())
                    .name(t.getName())
                    .rating(Optional.ofNullable(t.getAvgRating()).orElse(0.0))
                    .tags(tagNames)
                    .isOpenNow(available)
                    .address(t.getAddress())
                    .isOpen24h(t.getOpen24h())
                    .openTime(t.getOpenTime())
                    .closeTime(t.getCloseTime())
                    .distance((filter.getLat() != null && filter.getLng() != null)
                            ? distanceMeters(filter.getLat(), filter.getLng(), t.getLatitude(), t.getLongitude())
                            : null)
                    .mainImageUrl(mainImageUrl)
                    .build();
        }).toList();

        return MapResultResponse.builder()
                .totalCount(markers.size())
                .markers(markers)
                .build();

    }

    private List<Long> resolveTagIds(List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        List<String> normalized = names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) return List.of();
        return tagRepository.findIdsByNames(normalized);
    }

    // m 기준 하버사인 계산
    private int distanceMeters(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) return Integer.MAX_VALUE;
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return (int)Math.round(R * c);
    }
}
