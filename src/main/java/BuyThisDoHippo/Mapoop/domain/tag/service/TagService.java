package BuyThisDoHippo.Mapoop.domain.tag.service;

import BuyThisDoHippo.Mapoop.domain.map.dto.MarkerInfo;
import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.entity.ToiletTag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.TagRepository;
import BuyThisDoHippo.Mapoop.domain.tag.repository.ToiletTagRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {
    private static final String TAG_AVAILABLE_NOW = "현재이용가능";

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

    @Transactional
    public void syncTags(Toilet toilet, List<String> newTagNames) {
        Long toiletId = toilet.getId();
        List<String> names = normalizeNames(newTagNames);

        if (names.isEmpty()) {
            // 완전 삭제
            toiletTagRepository.deleteAll(toiletTagRepository.findByToiletId(toiletId));
            return;
        }

        // 1 존재하는 태그 조회
        List<Tag> targetTags = resolveTags(names);
        Set<Long> targetIds = targetTags.stream().map(Tag::getId).collect(Collectors.toSet());

        // 2 현재 연결된 태그 id 조회
        Set<Long> currentIds = toiletTagRepository.findTagIdsByToiletId(toiletId);

        // 3 삭제해야 하는 태그 id
        Set<Long> removeIds = new HashSet<>(currentIds);
        removeIds.removeAll(targetIds);
        if (!removeIds.isEmpty()) {
            toiletTagRepository.deleteByToiletIdAndTagIdIn(toiletId, removeIds);
        }

        // 4 최종 추가
        Set<Long> already = toiletTagRepository.findAttachedTagIds(toiletId, targetIds);
        List<ToiletTag> links = targetTags.stream()
                .filter(t -> !already.contains(t.getId()))
                .map(t -> ToiletTag.link(toilet, t))
                .toList();

        if (!links.isEmpty()) {
            toiletTagRepository.saveAll(links);
        }
    }

    private void appendAvailabilityTag(List<String> tags) {
        if (tags == null) return;
        if (!tags.contains(TAG_AVAILABLE_NOW)) {
            tags.add(TAG_AVAILABLE_NOW);
        }
    }

    private boolean computeIsOpenNow(Boolean open24h, LocalTime open, LocalTime close, LocalTime now) {
        if (Boolean.TRUE.equals(open24h)) return true;
        if (open == null || close == null || now == null) return false;
        if (open.equals(close)) return false;
        if (open.isBefore(close)) {
            return !now.isBefore(open) && now.isBefore(close);
        } else {
            return !now.isBefore(open) || now.isBefore(close);
        }
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

    public void addTagsToMarker(List<MarkerInfo> markers) {
        List<Long> ids = markers.stream().map(MarkerInfo::getToiletId).toList();
        if (ids.isEmpty()) return;

        Map<Long, List<String>> tagsById = toiletTagRepository.findTagNamesByToiletIds(ids).stream()
                .collect(Collectors.groupingBy(
                        ToiletTagRepository.ToiletIdTagName::getToiletId,
                        Collectors.mapping(ToiletTagRepository.ToiletIdTagName::getTagName, Collectors.toList())
                ));

        markers.forEach(m -> m.setTags(tagsById.getOrDefault(m.getToiletId(), List.of())));
    }
}
