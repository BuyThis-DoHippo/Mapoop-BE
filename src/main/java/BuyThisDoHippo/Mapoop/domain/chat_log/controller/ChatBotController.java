package BuyThisDoHippo.Mapoop.domain.chat_log.controller;

import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatAskRequest;
import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatHistoryResponse;
import BuyThisDoHippo.Mapoop.domain.chat_log.dto.ChatResponse;
import BuyThisDoHippo.Mapoop.domain.chat_log.service.ChatBotService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatBotController {

    private final ChatBotService chatBotService;

    /**
     * 챗봇 질문하기
     */
    @PostMapping("/ask")
    public ResponseEntity<CommonResponse<ChatResponse>> askQuestion(
            @RequestBody @Valid ChatAskRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            String sessionId = getSessionIdFromRequest(httpRequest);

            log.info("챗봇 질문 API 호출 - 사용자 ID: {}, 세션 ID: {}", userId, sessionId);

            ChatResponse response = chatBotService.askQuestion(userId, sessionId, request);

            return ResponseEntity.ok(
                    CommonResponse.onSuccess(response, "질문 응답 성공")
            );
        } catch (ApplicationException e) {
            log.error("챗봇 질문 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(CommonResponse.onFailure(null, CustomErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 챗봇 대화 내역 조회
     */
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<ChatHistoryResponse>> getChatHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            String sessionId = getSessionIdFromRequest(httpRequest);

            log.info("대화 내역 조회 API 호출 - 사용자 ID: {}, 페이지: {}", userId, page);

            ChatHistoryResponse response = chatBotService.getChatHistory(userId, sessionId, page, size);

            return ResponseEntity.ok(
                    CommonResponse.onSuccess(response, "대화 내역 조회 성공")
            );
        } catch (ApplicationException e) {
            log.error("대화 내역 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(CommonResponse.onFailure(null, e.getErrorCode()));
        } catch (Exception e) {
            log.error("대화 내역 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.onFailure(null, CustomErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 챗봇 대화 삭제
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<CommonResponse<Void>> deleteChatLog(
            @PathVariable Long chatId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            String sessionId = getSessionIdFromRequest(httpRequest);

            log.info("대화 삭제 API 호출 - 사용자 ID: {}, 채팅 ID: {}", userId, chatId);

            chatBotService.deleteChatLog(userId, sessionId, chatId);

            return ResponseEntity.ok(CommonResponse.onSuccess(null, "대화 내역이 삭제되었습니다"));
        } catch (ApplicationException e) {
            log.warn("대화 삭제 권한 없음 - 사용자 ID: {}, 채팅 ID: {}", getUserIdFromAuth(authentication), chatId);
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(CommonResponse.onFailure(null, e.getErrorCode()));
        } catch (Exception e) {
            log.error("대화 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.onFailure(null, CustomErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 인증 정보에서 사용자 ID 추출
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }

            return null;
        } catch (Exception e) {
            log.warn("사용자 ID 추출 실패", e);
            return null;
        }
    }

    /**
     * 요청에서 세션 ID 추출 (비로그인 사용자용)
     */
    private String getSessionIdFromRequest(HttpServletRequest request) {
        String sessionId = request.getHeader("X-Session-Id");

        if (sessionId == null) {
            sessionId = request.getSession().getId();
        }

        return sessionId;
    }

}
