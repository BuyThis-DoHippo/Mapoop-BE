package BuyThisDoHippo.Mapoop.domain.tag.service;

import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerDto;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final ToiletRepository toiletRepository;

    public void addTagsToToiletInfo(List<ToiletInfo> toiletInfos) {
        List<Long> toiletIds = toiletInfos.stream()
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
        toiletInfos.forEach(toiletInfo -> {
            List<String> tags = tagsByToiletId.getOrDefault(toiletInfo.getToiletId(), new ArrayList<>());
            toiletInfo.setTags(tags);
        });
    }

    public void addTagsToMarker(List<MarkerDto> markers) {
        List<Long> toiletIds = markers.stream()
                .map(MarkerDto::getToiletId)
                .toList();

        if(toiletIds.isEmpty()) return;

        List<ToiletTag> toiletTags = toiletRepository.findTagsByToiletIds(toiletIds);

        Map<Long, List<String>> tagsByToiletId = toiletTags.stream()
                .collect(Collectors.groupingBy(
                        tt -> tt.getToilet().getId(),
                        Collectors.mapping(tt -> tt.getTag().getName(), Collectors.toList())
                ));

        markers.forEach(toiletInfo -> {
            List<String> tags = tagsByToiletId.getOrDefault(toiletInfo.getToiletId(), new ArrayList<>());
            toiletInfo.setTags(tags);
        });

    }
}
