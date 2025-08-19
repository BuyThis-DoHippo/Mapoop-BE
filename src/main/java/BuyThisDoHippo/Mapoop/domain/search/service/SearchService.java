package BuyThisDoHippo.Mapoop.domain.search.service;

import BuyThisDoHippo.Mapoop.domain.search.dto.SearchSuggestionDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchFilter;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchResultResponse;
import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.TagRepository;
import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.global.common.TagConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SearchService {

    private final TagService tagService;
    private final ToiletRepository toiletRepository;
    private final TagRepository tagRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 캐시 키 접두사들
    private static final String AUTOCOMPLETE_PREFIX = "autocomplete:";
    private static final String SEARCH_COUNT_PREFIX = "search_count:";

    // 캐시 TTL 설정
    private static final int SHORT_TTL = 300;   // 5분 (일반 검색어)
    private static final int MEDIUM_TTL = 1800; // 30분 (인기 검색어)
    private static final int LONG_TTL = 3600;   // 1시간 (매우 인기 검색어)


    @Transactional(readOnly = true)
    public SearchResultResponse search(SearchFilter filter) {
        final String keyword = (filter.getKeyword() == null || filter.getKeyword().isBlank()) ? null : filter.getKeyword().trim();
        log.debug("검색 시작 - 검색어: {}", keyword);

        // 태그 id들 얻기
        List<Long> tagIds = resolveTagIds(filter.getTags());
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));

        List<Toilet> list = toiletRepository.searchAllFiltered(
                keyword,
                filter.getMinRating(),
                filter.getType(),
                tagIds.isEmpty() ? null : tagIds,
                tagIds.size(),
                Boolean.TRUE.equals(filter.getRequireAvailable()),
                now
        );

        // 정렬:
        //      위치 O → 거리 ASC
        //      위치 X → 평점 DESC
        //      → 이름 ASC
        Comparator<Toilet> comparator = filter.hasLocation()
                ? Comparator.comparingInt(t -> distanceMeters(
                filter.getLat(), filter.getLng(), t.getLatitude(), t.getLongitude()))
                : Comparator.<Toilet, Double>comparing(t -> Optional.ofNullable(t.getAvgRating()).orElse(0.0))
                .reversed()
                .thenComparing(Toilet::getName, Comparator.nullsLast(String::compareTo));
        list.sort(comparator);

        List<ToiletInfo> rows = list.stream().map(t -> {
            boolean available = t.isOpenNow(now);

            // 태그 이름들 세팅 (기존 db + 현재이용가능)
            List<String> tagNames = t.getToiletTags().stream()
                    .map(ToiletTag::getTag)
                    .filter(Objects::nonNull)
                    .map(Tag::getName)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            if (available)
                tagNames.add(TagConstants.VIRTUAL_AVAILABLE);

            return ToiletInfo.builder()
                    .toiletId(t.getId())
                    .name(t.getName())
                    .latitude(t.getLatitude())
                    .longitude(t.getLongitude())
                    .address(t.getAddress())
                    .rating(Optional.ofNullable(t.getAvgRating()).orElse(0.0))
                    .distance(filter.hasLocation()
                            ? distanceMeters(filter.getLat(), filter.getLng(), t.getLatitude(), t.getLongitude())
                            : null)
                    .tags(tagNames)
                    .isPartnership(Boolean.TRUE.equals(t.getIsPartnership()))
                    .build();
        }).toList();

        return SearchResultResponse.builder()
                .totalCount(rows.size())
                .toilets(rows)
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

    /** 하버사인(m) */
    private int distanceMeters(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) return Integer.MAX_VALUE;
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return (int)Math.round(R * c);
    }

    /**
     * 사용자가 타이핑 할 때 마다 호출
     */
    public List<SearchSuggestionDto> getAutoCompleteSuggestions(String keyword) {
        log.debug("자동완성 검색 - 쿼리: {}", keyword);

        if(keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        keyword = keyword.trim();
        String cacheKey = buildCacheKey(keyword);

        // Redis 캐시에서 1차 확인
        List<SearchSuggestionDto> cacheResults = getCacheSuggestions(cacheKey);
        if(cacheResults != null) {
            log.debug("캐시 Hit - Redis에서 가져옵니다.");
            return cacheResults;
        }

        // DB 2차 확인
        List<SearchSuggestionDto> suggestions = getDBSuggestions(keyword);
        log.debug("캐시 Miss - DB에서 가져옵니다.");

        // Redis에 저장
        int ttl = calculateDynamicTTL(keyword);
        cacheSuggestions(cacheKey, suggestions, ttl);

        return suggestions;
    }

    private void cacheSuggestions(String cacheKey, List<SearchSuggestionDto> suggestions, int ttl) {
        redisTemplate.opsForValue().set(cacheKey, suggestions, ttl, TimeUnit.SECONDS);
        log.debug("캐시 저장 완료 | key={} | ttl={}초 | size={}", cacheKey, ttl, suggestions.size());
    }

    private int calculateDynamicTTL(String keyword) {
        String countKey = SEARCH_COUNT_PREFIX + keyword.toLowerCase();
        Object countObj = redisTemplate.opsForValue().get(countKey);
        long searchCount = (countObj != null) ? Long.parseLong(String.valueOf(countObj)) : 0L;

        if (searchCount >= 50) return LONG_TTL;
        if (searchCount >= 10) return MEDIUM_TTL;
        return SHORT_TTL;
    }

    private List<SearchSuggestionDto> getDBSuggestions(String keyword) {
        Pageable pageable = PageRequest.of(0, 8);
        List<Toilet> toilets = toiletRepository
                .findByNameContainingIgnoreCaseOrderByAvgRatingDesc(keyword, pageable);

        return toilets.stream()
                .map(SearchSuggestionDto::from) // DTO 내부 from 사용
                .distinct()
                .limit(8)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<SearchSuggestionDto> getCacheSuggestions(String cacheKey) {
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> list) {
            return (List<SearchSuggestionDto>) list;
        }
        // List가 아니라면 캐시 미스
        return null;
    }

    private String buildCacheKey(String keyword) {
        return AUTOCOMPLETE_PREFIX + keyword.toLowerCase();
    }

    /**
     * 1. 홈화면 노출을 위한 가까운 화장실 리스트
     * 2. 긴급찾기 5곳을 찾기 위해 호출 (limit = 5)
     */
    public SearchResultResponse searchNearby(Double lat, Double lng, Integer limit) {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));

        List<Toilet> list = toiletRepository.findAll();
        List<Toilet> sliced;
        if (lat != null && lng != null) {
            // 위치 제공 → 거리순
            list.sort(Comparator.comparingInt(
                    t -> distanceMeters(lat, lng, t.getLatitude(), t.getLongitude())
            ));
            sliced = list.stream().limit(limit).toList();
        } else {
            // 위치 없음 → 평점순
            list.sort(
                    Comparator.<Toilet, Double>comparing(t -> Optional.ofNullable(t.getAvgRating()).orElse(0.0))
                            .reversed()
                            .thenComparing(Toilet::getName, Comparator.nullsLast(String::compareTo))
            );
            sliced = list.stream().limit(limit).toList();
        }

        List<ToiletInfo> rows = sliced.stream().map(t -> {
            boolean available = t.isOpenNow(now);

            List<String> tagNames = t.getToiletTags().stream()
                    .map(ToiletTag::getTag)
                    .filter(Objects::nonNull)
                    .map(Tag::getName)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            if (available) tagNames.add(TagConstants.VIRTUAL_AVAILABLE);

            return ToiletInfo.builder()
                    .toiletId(t.getId())
                    .name(t.getName())
                    .latitude(t.getLatitude())
                    .longitude(t.getLongitude())
                    .address(t.getAddress())
                    .rating(Optional.ofNullable(t.getAvgRating()).orElse(0.0))
                    .distance((lat != null && lng != null)
                            ? distanceMeters(lat, lng, t.getLatitude(), t.getLongitude())
                            : null) // 위치 없을 땐 null
                    .tags(tagNames)
                    .isPartnership(Boolean.TRUE.equals(t.getIsPartnership()))
                    .build();
        }).toList();

        return SearchResultResponse.builder()
                .totalCount(rows.size())
                .toilets(rows)
                .build();
    }

}
