package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatAskRequest;
import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatHistoryResponse;
import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatResponse;
import BuyThisDoHippo.Mapoop.domain.chat_log.entity.ChatLog;
import BuyThisDoHippo.Mapoop.domain.chat_log.repository.ChatLogRepository;
import BuyThisDoHippo.Mapoop.domain.search.dto.SearchFilter;
import BuyThisDoHippo.Mapoop.domain.search.service.SearchService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletInfo;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.domain.user.repository.UserRepository;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final ChatLogRepository chatLogRepository;
    // Kakao 폴백을 쓰지 않을 거면 주입도 제거
    // private final Optional<KakaoLocalService> kakaoLocalService;
    private final UserRepository userRepository;
    private final ChatGPTService chatGPTService;
    private final SearchService searchService;

    /** 챗봇에게 질문하고 답변받기: ✅ DB 후보만으로 추천 */
    @Transactional
    public ChatResponse askQuestion(Long userId, String sessionId, ChatAskRequest req) {
        log.info("챗봇 질문 요청 - userId={}, sessionId={}, q='{}'", userId, sessionId, req.getQuestion());

        User user = (userId != null) ? findUserById(userId) : null;

        // 규칙 기반 필터
        Double minRating = parseMinRating(req.getQuestion()).orElse(null);
        boolean accessibleOnly = parseAccessible(req.getQuestion());
        Integer maxMinutes = parseMaxMinutes(req.getQuestion()).orElse(null); // ⬅️ "2분", "5분" 같은 제한

        // "N분 이내" 요청인데 좌표가 없으면 계산이 불가 → 짧게 안내하고 반환
        if (maxMinutes != null && (req.getLat() == null || req.getLng() == null)) {
            String ans = "요청하신 \"도보 " + maxMinutes + "분 이내\"를 맞추려면 현재 위치(lat/lng)가 필요해요.";
            ChatLog saved = chatLogRepository.save(ChatLog.builder()
                    .question(req.getQuestion()).answer(ans).user(user).sessionId(sessionId).build());
            return ChatResponse.from(saved);
        }

        // DB 검색 (키워드 X, 조건만)
        SearchFilter filter = SearchFilter.builder()
                .keyword(null)
                .lat(req.getLat())
                .lng(req.getLng())
                .minRating(minRating)
                .requireAvailable(null)
                .build();

        var dbResult = searchService.search(filter);
        var toilets  = dbResult.getToilets();

        // ① N분 이내 필터링(있다면) ② 도보시간 오름차순 정렬 ③ 최대 5개
        List<ToiletInfo> picked = toilets.stream()
                .filter(t -> {
                    if (maxMinutes == null) return true;
                    int m = estimateWalkMinutes(req.getLat(), req.getLng(), t);
                    return m <= maxMinutes;
                })
                .sorted((a, b) -> Integer.compare(
                        estimateWalkMinutes(req.getLat(), req.getLng(), a),
                        estimateWalkMinutes(req.getLat(), req.getLng(), b)
                ))
                .limit(5)
                .toList();

        // 후보를 챗봇 포맷으로
        List<KakaoLocalService.PlaceDto> candidates = toPlaces(picked, req.getLat(), req.getLng());

        String answer;
        if (!candidates.isEmpty()) {
            // 최대 5개만 넘김(1개 이상이면 OK)
            answer = chatGPTService.recommendFromCandidates(
                    req.getQuestion(),
                    candidates,
                    candidates.size()
            );
        } else {
            answer = "요청하신 조건에 맞는 화장실을 DB에서 찾지 못했어요. 반경을 넓히거나 최소 별점을 낮춰보세요.";
        }

        ChatLog saved = chatLogRepository.save(ChatLog.builder()
                .question(req.getQuestion())
                .answer(answer)
                .user(user)
                .sessionId(sessionId)
                .build());

        log.info("챗봇 답변 완료 - chatId={}", saved.getId());
        return ChatResponse.from(saved);
    }


    /** 대화 내역 조회 */
    public ChatHistoryResponse getChatHistory(Long userId, String sessionId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatLog> chatLogs = (userId != null)
                ? chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : chatLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable);
        return ChatHistoryResponse.from(chatLogs.map(ChatResponse::from));
    }

    /** 특정 대화 삭제(권한 체크) */
    @Transactional
    public void deleteChatLog(Long userId, String sessionId, Long chatId) {
        boolean hasPermission = (userId != null)
                ? chatLogRepository.existsByIdAndUserId(chatId, userId)
                : chatLogRepository.existsByIdAndSessionId(chatId, sessionId);
        if (!hasPermission) throw new ApplicationException(CustomErrorCode.UNAUTHORIZED);
        chatLogRepository.deleteById(chatId);
    }

    /* ===================== Helper methods ===================== */

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.USER_NOT_FOUND));
    }

    /** ToiletInfo → 후보 포맷 */
    private List<KakaoLocalService.PlaceDto> toPlaces(List<ToiletInfo> list, Double userLat, Double userLng) {
        return list.stream().map(t -> new KakaoLocalService.PlaceDto(
                t.getName(),
                (t.getAddress() != null) ? t.getAddress() : "주소 정보 없음",
                floorLabel(t.getFloor()),
                walkTime(userLat, userLng, t),
                "정보 없음"
        )).toList();
    }

    private String floorLabel(Integer floor) {
        if (floor == null) return "";
        if (floor == 0) return "지상";
        if (floor < 0) return "지하" + Math.abs(floor) + "층";
        return floor + "층";
    }

    private String walkTime(Double userLat, Double userLng, ToiletInfo t) {
        if (t.getDistance() != null && t.getDistance() > 0) {
            double meters = t.getDistance();
            int minutes = Math.max(1, (int)Math.round(meters / 70.0));
            return "도보 약 " + minutes + "분";
        }
        if (userLat != null && userLng != null && t.getLatitude() != null && t.getLongitude() != null) {
            double meters = haversineMeters(userLat, userLng, t.getLatitude(), t.getLongitude());
            int minutes = Math.max(1, (int)Math.round(meters / 70.0));
            return "도보 약 " + minutes + "분";
        }
        return "도보 약 3분";
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private Optional<Double> parseMinRating(String q) {
        if (q == null) return Optional.empty();
        String s = q.toLowerCase();

        // 1) "별점 4.2", "평점 3.5"
        var m1 = Pattern.compile("(?:별점|평점)\\s*([0-5](?:\\.\\d)?)").matcher(s);
        if (m1.find()) {
            try { return Optional.of(Double.parseDouble(m1.group(1))); } catch (Exception ignored) {}
        }

        // 2) "4.0점", "4점 이상", "평점 4 이상"
        var m2 = Pattern.compile("([0-5](?:\\.\\d)?)\\s*점(?:\\s*이상)?").matcher(s);
        if (m2.find()) {
            try { return Optional.of(Double.parseDouble(m2.group(1))); } catch (Exception ignored) {}
        }

        // 3) "평점 4 이상"
        var m3 = Pattern.compile("(?:평점|별점)\\s*([0-5](?:\\.\\d)?)\\s*이상").matcher(s);
        if (m3.find()) {
            try { return Optional.of(Double.parseDouble(m3.group(1))); } catch (Exception ignored) {}
        }

        // 그 외 숫자(예: 5분, 2층 등)는 절대 평점으로 취급하지 않음
        return Optional.empty();
    }

    private boolean parseAccessible(String q) {
        if (q == null) return false;
        String s = q.toLowerCase();
        return s.contains("장애인") || s.contains("휠체어") || s.contains("배리어프리");
    }

    /** "2분", "5분거리", "도보 3분" 등에서 최대 분 추출 */
    private Optional<Integer> parseMaxMinutes(String q) {
        if (q == null) return Optional.empty();
        var m = Pattern.compile("(\\d{1,2})\\s*분").matcher(q);
        if (m.find()) {
            try { return Optional.of(Integer.parseInt(m.group(1))); } catch (Exception ignored) {}
        }
        return Optional.empty();
    }

    /** 도보 시간(분) 추정: distance 또는 좌표로 계산, 분당 70m 가정 */
    private int estimateWalkMinutes(Double userLat, Double userLng, ToiletInfo t) {
        double meters;
        if (t.getDistance() != null && t.getDistance() > 0) {
            meters = t.getDistance();  // ✅ 이미 미터 단위
        } else if (userLat != null && userLng != null && t.getLatitude() != null && t.getLongitude() != null) {
            meters = haversineMeters(userLat, userLng, t.getLatitude(), t.getLongitude());
        } else {
            meters = 200.0; // 기본 3분 정도
        }
        return Math.max(1, (int) Math.round(meters / 70.0));
    }
}
