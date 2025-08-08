package BuyThisDoHippo.Mapoop.domain.search.service;

import BuyThisDoHippo.Mapoop.domain.search.dto.SearchCriteriaDto;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchResultDto;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
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

    /**
     * 사용자가 검색 버튼을 클릭하거나 자동완성에서 선택했을 때 수행되는 로직
     * @param criteria 검색 조건
     */
    public SearchResultDto search(SearchCriteriaDto criteria) {
        String keyword = criteria.getKeyword();
        log.debug("검색 시작 - 검색어: {}", keyword);

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getPageSize());
        List<Toilet> toilets = toiletRepository.findByNameContainingIgnoreCaseOrderByAvgRatingDesc(keyword, pageable);

        // TODO: 필터링 로직들 추가 (최소 평점 등등 ..)

        List<ToiletInfo> toiletInfos = toilets.stream()
                .map(ToiletInfo::from)
                .toList();
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
}
