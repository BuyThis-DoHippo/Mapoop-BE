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

//    private final ToiletRepository toiletRepository;
    private final TagRepository tagRepository;
    private final ToiletTagRepository toiletTagRepository;

    // 태그 이름만 받아서 화장실-태그 연결
    @Transactional
    public void attachByNames(Toilet toilet, List<String> tagNames) {
        List<String> names = normalizeNames(tagNames);
        List<Tag> tags = resolveTags(names);
        attachTags(toilet, tags);
    }

    // 태그명 전처리
    @Transactional(readOnly = true)
    public List<String> normalizeNames(List<String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    // 태그 조회
    @Transactional(readOnly = true)
    public List<Tag> resolveTags(List<String> names) {
        if (names == null || names.isEmpty())
            return Collections.emptyList();

        List<Tag> found = tagRepository.findAllByNameIn(names);

        if (found.size() != names.size()) {
            throw new ApplicationException(CustomErrorCode.TAG_NOT_FOUND);
        }
        return found;
    }

    @Transactional
    public void attachTags(Toilet toilet, List<Tag> tags) {
        Long toiletId = toilet.getId();
        if (tags == null || tags.isEmpty()) return;

        Set<Long> targetTagIds = tags.stream()
                .map(Tag::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (targetTagIds.isEmpty()) return;

        Set<Long> already = toiletTagRepository.findAttachedTagIds(toiletId, targetTagIds);

        List<ToiletTag> links = tags.stream()
                .filter(t -> t.getId() != null && !already.contains(t.getId()))
                .map(t -> ToiletTag.link(toilet, t))
                .toList();

        if (!links.isEmpty()) {
            toiletTagRepository.saveAll(links);
        }
        log.debug("태그 연결 완료");
    }


    public void addTagsToToiletInfo(List<ToiletInfo> toiletInfos) {
        List<Long> ids = toiletInfos.stream().map(ToiletInfo::getToiletId).toList();
        if (ids.isEmpty()) return;

        Map<Long, List<String>> tagsById = toiletTagRepository.findTagNamesByToiletIds(ids).stream()
                .collect(Collectors.groupingBy(
                        ToiletTagRepository.ToiletIdTagName::getToiletId,
                        Collectors.mapping(ToiletTagRepository.ToiletIdTagName::getTagName, Collectors.toList())
                ));

        toiletInfos.forEach(info -> info.setTags(tagsById.getOrDefault(info.getToiletId(), List.of())));
    }

    public void addTagsToMarker(List<MarkerDto> markers) {
        List<Long> ids = markers.stream().map(MarkerDto::getToiletId).toList();
        if (ids.isEmpty()) return;

        Map<Long, List<String>> tagsById = toiletTagRepository.findTagNamesByToiletIds(ids).stream()
                .collect(Collectors.groupingBy(
                        ToiletTagRepository.ToiletIdTagName::getToiletId,
                        Collectors.mapping(ToiletTagRepository.ToiletIdTagName::getTagName, Collectors.toList())
                ));

        markers.forEach(m -> m.setTags(tagsById.getOrDefault(m.getToiletId(), List.of())));
    }
}
