package BuyThisDoHippo.Mapoop.domain.tag.service;

import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerDto;
import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.TagRepository;
import BuyThisDoHippo.Mapoop.domain.tag.repository.ToiletTagRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final ToiletRepository toiletRepository;
    private final TagRepository tagRepository;
    private final ToiletTagRepository toiletTagRepository;

    @Transactional
    public void attachTags(Long toiletId, List<String> tags) {
        log.debug("화장실 - 태그 설정 요청");
        if (tags == null || tags.isEmpty()) return;

        // 입력 받은 태그들 전처리 과정
        List<String> names = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (names.isEmpty()) return;

        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.TOILET_NOT_FOUND));

        List<Tag> existing = tagRepository.findAllByNameIn(names);
        Set<String> foundNames = existing.stream().map(Tag::getName).collect(Collectors.toSet());
        List<String> missing = names.stream().filter(n -> !foundNames.contains(n)).toList();
        if (!missing.isEmpty()) {
            throw new ApplicationException(CustomErrorCode.TAG_NOT_FOUND);
        }

        Set<Long> alreadyAttachedTagIds =
                toiletTagRepository.findByToiletId(toiletId).stream()
                        .map(tt -> tt.getTag().getId())
                        .collect(Collectors.toSet());

        List<ToiletTag> toSave = existing.stream()
                .filter(tag -> !alreadyAttachedTagIds.contains(tag.getId()))
                .map(tag -> ToiletTag.builder().toilet(toilet).tag(tag).build())
                .toList();

        if (!toSave.isEmpty()) {
            toiletTagRepository.saveAll(toSave);
        }

        log.debug("화장실 - 태그 설정 완료!");
    }

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
