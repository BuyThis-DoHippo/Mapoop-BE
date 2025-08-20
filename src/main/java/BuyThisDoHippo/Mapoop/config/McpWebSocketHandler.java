package BuyThisDoHippo.Mapoop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP WebSocket 연결 핸들러
 */
@Slf4j
public class McpWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("MCP WebSocket 연결 성공: {}", session.getId());
        sessions.put(session.getId(), session);
        
        // MCP 초기화 메시지 전송
        McpMessage initMessage = McpMessage.builder()
            .method("initialize")
            .params(McpInitParams.builder()
                .protocolVersion("2024-11-05")
                .capabilities(McpCapabilities.builder()
                    .tools(true)
                    .resources(true)
                    .build())
                .clientInfo(McpClientInfo.builder()
                    .name("Mapoop-BE")
                    .version("1.0.0")
                    .build())
                .build())
            .build();
            
        sendMessage(session, initMessage);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            log.info("MCP 메시지 수신: {}", textMessage.getPayload());
            
            try {
                McpMessage mcpMessage = objectMapper.readValue(textMessage.getPayload(), McpMessage.class);
                handleMcpMessage(session, mcpMessage);
            } catch (Exception e) {
                log.error("MCP 메시지 파싱 실패", e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("MCP WebSocket 전송 오류: {}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("MCP WebSocket 연결 종료: {}, 상태: {}", session.getId(), closeStatus);
        sessions.remove(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void handleMcpMessage(WebSocketSession session, McpMessage message) {
        // MCP 메시지 처리 로직
        switch (message.getMethod()) {
            case "initialize" -> handleInitialize(session, message);
            case "tools/list" -> handleToolsList(session, message);
            case "tools/call" -> handleToolsCall(session, message);
            case "resources/list" -> handleResourcesList(session, message);
            default -> log.warn("알 수 없는 MCP 메서드: {}", message.getMethod());
        }
    }

    private void handleInitialize(WebSocketSession session, McpMessage message) {
        // 초기화 응답 처리
        log.info("MCP 초기화 완료");
    }

    private void handleToolsList(WebSocketSession session, McpMessage message) {
        // 도구 목록 요청 처리
        log.info("MCP 도구 목록 요청");
    }

    private void handleToolsCall(WebSocketSession session, McpMessage message) {
        // 도구 호출 처리
        log.info("MCP 도구 호출: {}", message);
    }

    private void handleResourcesList(WebSocketSession session, McpMessage message) {
        // 리소스 목록 요청 처리
        log.info("MCP 리소스 목록 요청");
    }

    public void sendMessage(WebSocketSession session, McpMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("MCP 메시지 전송: {}", jsonMessage);
        } catch (IOException e) {
            log.error("MCP 메시지 전송 실패", e);
        }
    }

    public void broadcastMessage(McpMessage message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        });
    }
}
