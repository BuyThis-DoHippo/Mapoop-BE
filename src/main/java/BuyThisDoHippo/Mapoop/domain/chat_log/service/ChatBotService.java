package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatAskRequest;
import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatHistoryResponse;
import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatResponse;
import BuyThisDoHippo.Mapoop.domain.chat_log.entity.ChatLog;
import BuyThisDoHippo.Mapoop.domain.chat_log.repository.ChatLogRepository;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.domain.user.repository.UserRepository;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatBotService {

    private final ChatLogRepository chatLogRepository;
    private final KakaoLocalService kakaoLocalService;
    private final UserRepository userRepository;
    private final ChatGPTService chatGPTService;  // GPT 서비스 추가

    /**
     * 챗봇에게 질문하고 답변받기
     */
    @RequiredArgsConstructor
    @Service
    public class ChatBotService {

        private final SearchService searchService;          // ✅ 추가
        private final ChatGPTService chatGPTService;
        private final KakaoLocalService kakaoLocalService;  // 폴백용(선택)

        @Transactional
        public ChatResponse askQuestion(Long userId, String sessionId, ChatAskRequest req) {
            User user = (userId != null) ? findUserById(userId) : null;

            // 1) 자연어에서 간단한 필터 뽑기 (선택)
            Double minRating = parseMinRating(req.getQuestion()).orElse(req.getMinRating());
            boolean accessibleOnly = parseAccessible(req.getQuestion()) || Boolean.TRUE.equals(req.getAccessibleOnly());

            // 2) 우리 DB 우선 검색
            var filter = SearchFilterDto.builder()
                    .keyword(req.getQuestion())     // 단순히 질문 전체를 키워드로 먼저 넣고
                    .minRating(minRating)           // 별점 필터
                    .hasAccessibleToilet(accessibleOnly ? true : null)
                    // 필요시 다른 필터도 매핑
                    .page(0).pageSize(5)
                    .build();

            var dbResult = searchService.search(filter, req.getLat(), req.getLng());
            var toilets = dbResult.getToilets(); // List<ToiletInfo>

            List<KakaoLocalService.PlaceDto> places = toPlaces(toilets, req.getLat(), req.getLng());

            // 3) DB 결과 없고 좌표 있으면 → 카카오 폴백(비즈월렛 준비 전이면 생략 가능)
            if (places.isEmpty() && req.getLat() != null && req.getLng() != null) {
                places = kakaoLocalService.searchToilets(req.getLat(), req.getLng(),
                        req.getRadius() == null ? 500 : req.getRadius());
            }

            // 4) GPT 호출 (places 있으면 포맷만 하게)
            String answer = chatGPTService.generateChatResponse(
                    req.getQuestion(),
                    user,
                    places.isEmpty() ? null : places
            );

            // 5) 저장/반환
            ChatLog saved = chatLogRepository.save(ChatLog.builder()
                    .question(req.getQuestion())
                    .answer(answer)
                    .user(user)
                    .sessionId(sessionId)
                    .build());

            return ChatResponse.from(saved);
        }

        private Optional<Double> parseMinRating(String q) {
            if (q == null) return Optional.empty();
            // “3.5”, “별점 4”, “평점4이상” 등 간단 추출
            var m = java.util.regex.Pattern.compile("(?:별점|평점)?\\s*([0-5](?:\\.\\d)?)\\s*(?:점|이상)?")
                    .matcher(q);
            if (m.find()) {
                try { return Optional.of(Double.parseDouble(m.group(1))); } catch (Exception ignored) {}
            }
            return Optional.empty();
        }
        private boolean parseAccessible(String q) {
            if (q == null) return false;
            return q.contains("장애인") || q.contains("휠체어") || q.contains("배리어프리");
        }
    }


    /**
     * 사용자의 대화 내역 조회 (페이지네이션)
     */
    public ChatHistoryResponse getChatHistory(Long userId, String sessionId, int page, int size) {
        log.info("대화 내역 조회 - 사용자 ID: {}, 세션 ID: {}, 페이지: {}", userId, sessionId, page);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ChatLog> chatLogs;

        if(userId != null) {
            chatLogs = chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            chatLogs = chatLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable);
        }

        Page<ChatResponse> chatResponses = chatLogs.map(ChatResponse::from);

        return ChatHistoryResponse.from(chatResponses);
    }

    /**
     * 특정 대화 삭제(권한 체크 포함)
     */
    @Transactional
    public void deleteChatLog(Long userId, String sessionId, Long chatId) {
        log.info("대화 삭제 요청 - 사용자 ID: {}, 세션 ID: {}, 채팅 ID: {}", userId, sessionId, chatId);

        boolean hasPermission = false;

        if (userId != null) {
            hasPermission = chatLogRepository.existsByIdAndUserId(chatId, userId);
        } else {
            hasPermission = chatLogRepository.existsByIdAndSessionId(chatId, sessionId);
        }

        if (!hasPermission) {
            throw new ApplicationException(CustomErrorCode.UNAUTHORIZED);
        }

        chatLogRepository.deleteById(chatId);
        log.info("대화 삭제 완료 - 채팅 ID: {}", chatId);
    }

    /**
     * 챗봇 답변 생성 로직 (규칙 기반)
     * TODO: 나중에 GPT API로 개선
     */
    private String generateChatBotAnswer(String question, User user) {
        String lowerQuestion = question.toLowerCase();

        // 인사 관련
        if (containsAny(lowerQuestion, "안녕", "hi", "hello", "처음")) {
            return "안녕하세요! 화장실 찾기 도우미 마푸프입니다. 어떤 화장실을 찾고 계신가요?";
        }

        // 위치 관련 질문
        if (containsAny(lowerQuestion, "가까운", "근처", "찾아", "어디")) {
            return String.format("현재 위치에서 가장 가까운 화장실은 강남역 지하 1층 화장실입니다. " +
                            "도보로 약 3분 거리에 있으며, 24시간 이용 가능합니다.%s",
                    user != null ? " 더 정확한 위치는 지도에서 확인해보세요!" : "");
        }

        // 시간 관련 질문
        if (containsAny(lowerQuestion, "24시간", "밤", "새벽", "언제", "시간")) {
            return "24시간 이용 가능한 화장실을 안내해드립니다:\n" +
                    "• 지하철역 (1호선~9호선 대부분)\n" +
                    "• 24시간 편의점 (CU, GS25, 세븐일레븐)\n" +
                    "• 일부 공공시설 및 병원";
        }

        // 청결 관련 질문
        if (containsAny(lowerQuestion, "깨끗한", "청결", "더러운", "냄새")) {
            return "청결도가 높은 화장실을 추천드립니다! " +
                    "최근 리뷰에서 청결도 4.5점 이상을 받은 화장실들을 확인해보세요. " +
                    "백화점이나 대형마트의 화장실이 일반적으로 깨끗합니다.";
        }

        // 접근성 관련 질문
        if (containsAny(lowerQuestion, "장애인", "휠체어", "접근", "경사로", "엘리베이터")) {
            return "장애인 접근 가능한 화장실을 안내해드립니다:\n" +
                    "• 휠체어 이용 가능한 넓은 공간\n" +
                    "• 손잡이 및 비상벨 설치\n" +
                    "• 낮은 세면대 구비\n" +
                    "지하철역과 공공시설에 잘 갖춰져 있습니다.";
        }

        // 아기 관련 질문
        if (containsAny(lowerQuestion, "아기", "기저귀", "수유", "육아")) {
            return "육아맘을 위한 화장실 정보를 안내드립니다:\n" +
                    "• 기저귀 교환대 구비\n" +
                    "• 수유실 인근 위치\n" +
                    "• 넓은 공간으로 유모차 이용 가능\n" +
                    "백화점과 대형마트에서 이용하시기 편리합니다.";
        }

        // 감사 인사
        if (containsAny(lowerQuestion, "고마워", "감사", "도움", "잘했어")) {
            return "도움이 되었다니 기뻐요! 언제든지 화장실 찾기가 필요하시면 말씀해주세요. 😊";
        }

        // 기본 답변
        return "죄송합니다. 해당 질문에 대한 정확한 답변을 드리기 어렵습니다. " +
                "다음과 같은 질문을 해보세요:\n" +
                "• \"가까운 화장실 어디에 있어?\"\n" +
                "• \"24시간 이용 가능한 곳 있어?\"\n" +
                "• \"깨끗한 화장실 추천해줘\"\n" +
                "• \"장애인 접근 가능한 곳 알려줘\"";
    }

    /**
     * 문자열에 키워드가 포함되어 있는지 확인
     */
    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    /**
     * 사용자 조회 (로그인 사용자)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.USER_NOT_FOUND));
    }

    // ChatBotService 내부 헬퍼
    private List<KakaoLocalService.PlaceDto> toPlaces(List<ToiletInfo> list, Double userLat, Double userLng) {
        return list.stream().map(t -> new KakaoLocalService.PlaceDto(
                t.getName(),
                t.getRoadAddress() != null ? t.getRoadAddress() : t.getJibunAddress(),
                safeFloor(t.getBuildingFloor()),                 // 없으면 ""
                calcWalkTime(userLat, userLng, t.getLat(), t.getLng()), // 좌표 있으면 도보 n분, 없으면 기본
                t.getHours() != null ? t.getHours() : "정보 없음"
        )).toList();
    }

    private String safeFloor(String floor) {
        if (floor == null) return "";
        return floor; // 필요시 “B1/지하1층/2층” 통일 규칙 적용
    }

    private String calcWalkTime(Double uLat, Double uLng, Double tLat, Double tLng) {
        if (uLat == null || uLng == null || tLat == null || tLng == null) return "도보 약 3분";
        double meters = haversineMeters(uLat, uLng, tLat, tLng);
        int minutes = Math.max(1, (int)Math.round(meters / 70.0)); // 분당 70m 가정
        return "도보 약 " + minutes + "분";
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // m
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }


}

