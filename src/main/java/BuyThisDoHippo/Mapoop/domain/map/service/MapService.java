package BuyThisDoHippo.Mapoop.domain.map.service;

import BuyThisDoHippo.Mapoop.domain.map.dto.MapResultDto;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerDto;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerFilterDto;
import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.GenderType;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {

    private final TagService tagService;
    private final ToiletRepository toiletRepository;

    public MapResultDto getMapResult(MarkerFilterDto filter) {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));

        List<MarkerDto> markers = toiletRepository.findMarkers(
                filter.getMinRating(),
                filter.getType(),
                filter.getGenderType(),
                filter.getHasAccessibleToilet(),
                filter.getHasDiaperTable(),
                filter.getIsAvailable(),
                filter.getIsOpen24h(),
                filter.getHasBidet(),
                filter.getHasIndoorToilet(),
                filter.getProvidesSanitaryItems(),
                now
        );
        tagService.addTagsToMarker(markers);

        long totalCount = toiletRepository.countMarkers(
                filter.getMinRating(),
                filter.getType(),
                filter.getGenderType(),
                filter.getHasAccessibleToilet(),
                filter.getHasDiaperTable(),
                filter.getIsAvailable(),
                filter.getIsOpen24h(),
                filter.getHasBidet(),
                filter.getHasIndoorToilet(),
                filter.getProvidesSanitaryItems(),
                now
        );

        return MapResultDto.builder()
                .totalCount(totalCount)
                .markers(markers)
                .build();
    }
}
