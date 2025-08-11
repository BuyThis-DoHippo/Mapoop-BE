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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatBotService {

    private final ChatLogRepository chatLogRepository;
    private final UserRepository userRepository;
    private final OpenAIService openAIService;  // GPT 서비스 추가

    /**
     * 챗봇에게 질문하고 답변받기
     */
    @Transactional
    public ChatResponse askQuestion(Long userId, String sessionId, ChatAskRequest request) {
        log.info("챗봇 질문 요청 - 사용자 ID: {}, 세션 ID: {}, 질문: {}",
                userId, sessionId, request.getQuestion());

        User user = null;
        if (userId != null) {
            user = findUserById(userId);
        }

        // 2. GPT로 답변 생성 🤖
        String answer = openAIService.generateChatResponse(request.getQuestion(), user);

        ChatLog chatLog = ChatLog.builder()
                .question(request.getQuestion())
                .answer(answer)
                .user(user)
                .sessionId(sessionId)
                .build();

        ChatLog savedChatLog = chatLogRepository.save(chatLog);

        log.info("챗봇 답변 완료 - 채팅 ID: {}", savedChatLog.getId());

        return ChatResponse.from(savedChatLog);
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

}
