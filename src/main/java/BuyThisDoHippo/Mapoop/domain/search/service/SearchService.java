package BuyThisDoHippo.Mapoop.domain.search.service;

import BuyThisDoHippo.Mapoop.domain.search.dto.SearchHomeDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchSuggestionDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchFilterDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchResultDto;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.GenderType;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.ToiletType;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.projection.ToiletWithDistance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final TagService tagService;
    private final ToiletRepository toiletRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 캐시 키 접두사들
    private static final String AUTOCOMPLETE_PREFIX = "autocomplete:";
    private static final String SEARCH_COUNT_PREFIX = "search_count:";

    // 캐시 TTL 설정
    private static final int SHORT_TTL = 300;   // 5분 (일반 검색어)
    private static final int MEDIUM_TTL = 1800; // 30분 (인기 검색어)
    private static final int LONG_TTL = 3600;   // 1시간 (매우 인기 검색어)

    // 지구 반지름 (km)
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * 사용자가 검색 버튼을 클릭하거나 자동완성에서 선택했을 때 수행되는 로직
     * 1. 위치 제공 X -> 키워드+필터 -> 거리순 정렬 -> 페이징
     * 2. 위치 제공 O -> 키워드+필터 -> 평점순 정렬 -> 페이징
     * @param filter 검색 조건
     */
    public SearchResultDto search(SearchFilterDto filter, Double lat, Double lng) {
        final String keyword = (filter.getKeyword() == null || filter.getKeyword().isBlank()) ? null : filter.getKeyword();
        log.debug("검색 시작 - 검색어: {}", keyword);
        final Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize());

        // “현재 이용 가능” 계산용 now (HH:mm:ss.nn)
        final String nowTime = java.time.LocalTime.now().toString();
        // MySQL TIME 비교 안정화를 위해 초 단위까지만 자르기
        final String nowHHmmss = nowTime.length() >= 8 ? nowTime.substring(0, 8) : nowTime;

        Page<ToiletWithDistance> toilets;

        if (lat != null && lng != null) {
            // 위치 허용 → 거리순
            toilets = toiletRepository.searchByDistance(
                    keyword,
                    filter.getMinRating(),
                    filter.getToiletType(),
                    filter.getGenderType(),
                    filter.getIsOpen24h(),
                    filter.getHasIndoorToilet(),
                    filter.getHasBidet(),
                    filter.getHasAccessibleToilet(),
                    filter.getHasDiaperTable(),
                    filter.getProvidesSanitaryItems(),
                    filter.getIsAvailable(),
                    nowHHmmss,
                    lat, lng,
                    pageable
            );
        } else {
            // 위치 미허용 → 평점순
            toilets = toiletRepository.searchByRating(
                    keyword,
                    filter.getMinRating(),
                    filter.getToiletType(),
                    filter.getGenderType(),
                    filter.getIsOpen24h(),
                    filter.getHasIndoorToilet(),
                    filter.getHasBidet(),
                    filter.getHasAccessibleToilet(),
                    filter.getHasDiaperTable(),
                    filter.getProvidesSanitaryItems(),
                    filter.getIsAvailable(),
                    nowHHmmss,
                    pageable
            );
        }

        // Entity -> Dto
        List<ToiletInfo> toiletInfos = toilets.getContent().stream()
                .map(ToiletInfo::fromProjection)
                .toList();
        tagService.addTagsToToiletInfo(toiletInfos);

        return SearchResultDto.builder()
                .totalCount(toilets.getTotalElements())
                .toilets(toiletInfos)
                .currentPage(filter.getPage())
                .totalPages(toilets.getTotalPages())
                .build();
    }

    private String safeToiletType(ToiletType type) {
        return (type == null) ? null : type.name();
    }

    private String safeGenderType(GenderType genderType) {
        return (genderType == null) ? null : genderType.name();
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
     * 2. 긴급찾기 3곳을 찾기 위해 호출 (limit == 3)
     */
    public SearchHomeDto searchNearby(Double userLat, Double userLng, Double radiusKm, Integer limit) {

        // 1. null 아니면 가까운 순으로 sort 하고 return
        if(userLat != null && userLng != null) {
            List<ToiletWithDistance> nearbyToilets = toiletRepository.findNearbyToilets(userLat, userLng, radiusKm, limit);
            List<ToiletInfo> nearbyToiletInfos = nearbyToilets.stream()
                    .map(ToiletInfo::fromProjection)
                    .toList();
            tagService.addTagsToToiletInfo(nearbyToiletInfos);

            long totalCount = nearbyToiletInfos.size();
            return SearchHomeDto.builder()
                    .totalCount(totalCount)
                    .toilets(nearbyToiletInfos)
                    .limit(limit)
                    .radiusKm(radiusKm)
                    .build();

        } else {
            // 2. null 이면 바로 rating 순으로 찾아서 return
            Pageable pageable = PageRequest.of(0, limit);
            List<Toilet> ratingToilets = toiletRepository.findAllByOrderByAvgRatingDesc(pageable);
            List<ToiletInfo> ratingToiletInfos = ratingToilets.stream()
                    .map(ToiletInfo::from)
                    .toList();
            long totalCount = ratingToiletInfos.size();
            return SearchHomeDto.builder()
                    .totalCount(totalCount)
                    .toilets(ratingToiletInfos)
                    .limit(limit)
                    .radiusKm(radiusKm)
                    .build();
        }
    }

}
