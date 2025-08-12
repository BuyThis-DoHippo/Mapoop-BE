package BuyThisDoHippo.Mapoop.domain.map.service;

import BuyThisDoHippo.Mapoop.domain.map.dto.MapResultDto;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerDto;
import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerFilterDto;
import BuyThisDoHippo.Mapoop.domain.tag.service.TagService;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.GenderType;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
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

        final GenderType genderType = decideGenderType(filter.getIsGenderSeparated());
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));

        List<MarkerDto> markers = toiletRepository.findMarkers(
                filter.getMinRating(),
                filter.getType(),
                genderType,
                filter.getHasAccessibleToilet(),
                filter.getHasDiaperTable(),
                filter.getIsAvailable(),
                now
        );
        tagService.addTagsToMarker(markers);

        long totalCount = toiletRepository.countMarkers(
                filter.getMinRating(),
                filter.getType(),
                genderType,
                filter.getHasAccessibleToilet(),
                filter.getHasDiaperTable(),
                filter.getIsAvailable(),
                now
        );

        return MapResultDto.builder()
                .totalCount(totalCount)
                .markers(markers)
                .build();
    }

    private GenderType decideGenderType(Boolean isGenderSeparated) {
        if (isGenderSeparated == null) return null;
        return isGenderSeparated ? GenderType.SEPARATE : GenderType.UNISEX;
    }
}
