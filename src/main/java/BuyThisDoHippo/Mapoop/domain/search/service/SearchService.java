package BuyThisDoHippo.Mapoop.domain.search.service;

import BuyThisDoHippo.Mapoop.domain.search.dto.SearchHomeDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchSuggestionDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchCriteriaDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchResultDto;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.projection.ToiletWithDistance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * @param criteria 검색 조건
     */
    public SearchResultDto search(SearchCriteriaDto criteria, Double lat, Double lng) {
        String keyword = criteria.getKeyword();
        log.debug("검색 시작 - 검색어: {}", keyword);

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getPageSize());
        List<Toilet> toilets = toiletRepository.findByNameContainingIgnoreCaseOrderByAvgRatingDesc(keyword, pageable);

        // TODO: 필터링 로직들 추가 (최소 평점 등등 ..) + distance 계산

        // Entity -> Dto
        List<ToiletInfo> toiletInfos = toilets.stream()
                .map(ToiletInfo::from)
                .toList();

        // 현재 좌표가 들어왔다면 거리 계산 + 거리순으로 sorting
        if (lat != null && lng != null) {
            for (ToiletInfo info : toiletInfos) {
                Double toiletLat = info.getLatitude();
                Double toiletLng = info.getLongitude();

                if (toiletLat != null && toiletLng != null) {
                    Double distance = distanceKilometers(lat, lng, toiletLat, toiletLng);
                    info.setDistance(distance);
                } else {
                    info.setDistance(null);
                }
            }

            toiletInfos = toiletInfos.stream()
                    .sorted(Comparator.comparing(
                         ti -> Optional.ofNullable(ti.getDistance()).orElse(Double.MAX_VALUE)))
                    .toList();
        }

        long totalCount = toiletInfos.size();
        int totalPages = (int) Math.ceil((double) totalCount / criteria.getPageSize());
        log.debug("검색 완료 - 결과 수: {}", totalCount);

        return SearchResultDto.builder()
                .totalCount(totalCount)
                .toilets(toiletInfos)
                .currentPage(criteria.getPage())
                .totalPages(totalPages)
                .build();

    }

    /**
     * 하버사인 공식 기반 거리 계산 (km 단위)
     */
    private static double distanceKilometers(double lat1, double lon1, double lat2, double lon2) {
        // 위도/경도 -> 라디안 변환
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        // Haversine
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // km 단위로 반환 (EARTH_RADIUS_KM = 6371.0)
        return EARTH_RADIUS_KM * c;
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
     * lat, lng가 null일 경우 평점순 제공
     * 2. 긴급찾기 3곳을 찾기 위해 호출 (limit == 3)
     */
    public SearchHomeDto searchNearby(Double userLat, Double userLng, Double radiusKm, Integer limit) {

        // 1. null 아니면 가까운 순으로 sort 하고 return
        if(userLat != null && userLng != null) {
            List<ToiletWithDistance> nearbyToilets = toiletRepository.findNearbyToilets(userLat, userLng, radiusKm, limit);
            List<ToiletInfo> nearbyToiletInfos = nearbyToilets.stream()
                    .map(this::mapToToiletInfo)
                    .toList();
            addTagsToToiletInfo(nearbyToiletInfos);

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

    private void addTagsToToiletInfo(List<ToiletInfo> nearbyToiletInfos) {
        List<Long> toiletIds = nearbyToiletInfos.stream()
                .map(ToiletInfo::getToiletId)
                .toList();

        if(toiletIds.isEmpty()) return;

        List<ToiletTag> toiletTags = toiletRepository.findTagsByToiletIds(toiletIds);

        // toiletId별로 태그들을 그룹화
        Map<Long, List<String>> tagsByToiletId = toiletTags.stream()
                .collect(Collectors.groupingBy(
                        tt -> tt.getToilet().getId(),
                        Collectors.mapping(tt -> tt.getTag().getName(), Collectors.toList())
                ));

        // ToiletInfo에 태그 설정
        nearbyToiletInfos.forEach(toiletInfo -> {
            List<String> tags = tagsByToiletId.getOrDefault(toiletInfo.getToiletId(), new ArrayList<>());
            toiletInfo.setTags(tags);
        });
    }

    private ToiletInfo mapToToiletInfo(ToiletWithDistance projection) {
        return ToiletInfo.builder()
                .toiletId(projection.getId())
                .name(projection.getName())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .address(projection.getAddress())
                .floor(projection.getFloor())
                .rating(projection.getAvgRating())
                .distance(projection.getDistance())
                .tags(new ArrayList<>()) // 나중에 추가
                .isPartnership(projection.getIsPartnership())
                .build();
    }

}
