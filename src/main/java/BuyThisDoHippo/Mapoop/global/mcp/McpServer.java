package BuyThisDoHippo.Mapoop.global.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) 서버 구현
 * AI 모델이 외부 도구나 시스템과 상호작용할 수 있게 해주는 WebSocket 기반 서버
 */
@Component
public class McpServer extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions;
    private final McpToolRegistry toolRegistry;
    
    public McpServer(ObjectMapper objectMapper, McpToolRegistry toolRegistry) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.sessions = new ConcurrentHashMap<>();
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        sendMessage(session, createInitializeResponse());
        System.out.println("MCP Client connected: " + session.getId());
    }
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode request = objectMapper.readTree(message.getPayload());
            String method = request.get("method").asText();
            
            switch (method) {
                case "initialize" -> handleInitialize(session, request);
                case "tools/list" -> handleToolsList(session, request);
                case "tools/call" -> handleToolCall(session, request);
                case "resources/list" -> handleResourcesList(session, request);
                case "resources/read" -> handleResourceRead(session, request);
                default -> sendErrorResponse(session, request.get("id"), "Unknown method: " + method);
            }
        } catch (Exception e) {
            System.err.println("Error handling MCP message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleInitialize(WebSocketSession session, JsonNode request) throws IOException {
        var response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", request.get("id").asText());
        
        var result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", objectMapper.createObjectNode()
            .put("name", "Mapoop MCP Server")
            .put("version", "1.0.0"));
        
        var capabilities = objectMapper.createObjectNode();
        capabilities.put("tools", objectMapper.createObjectNode());
        capabilities.put("resources", objectMapper.createObjectNode());
        result.set("capabilities", capabilities);
        
        response.set("result", result);
        sendMessage(session, response);
    }
    
    private void handleToolsList(WebSocketSession session, JsonNode request) throws IOException {
        var response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", request.get("id").asText());
        
        var result = objectMapper.createObjectNode();
        var tools = objectMapper.createArrayNode();
        
        // 사용 가능한 도구들을 등록
        for (McpTool tool : toolRegistry.getTools()) {
            var toolNode = objectMapper.createObjectNode();
            toolNode.put("name", tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", tool.getInputSchema());
            tools.add(toolNode);
        }
        
        result.set("tools", tools);
        response.set("result", result);
        sendMessage(session, response);
    }
    
    private void handleToolCall(WebSocketSession session, JsonNode request) throws IOException {
        var params = request.get("params");
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");
        
        try {
            McpTool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                sendErrorResponse(session, request.get("id"), "Tool not found: " + toolName);
                return;
            }
            
            Object result = tool.execute(arguments);
            
            var response = objectMapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.put("id", request.get("id").asText());
            
            var resultNode = objectMapper.createObjectNode();
            var content = objectMapper.createArrayNode();
            var textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", objectMapper.writeValueAsString(result));
            content.add(textContent);
            resultNode.set("content", content);
            
            response.set("result", resultNode);
            sendMessage(session, response);
            
        } catch (Exception e) {
            sendErrorResponse(session, request.get("id"), "Tool execution error: " + e.getMessage());
        }
    }
    
    private void handleResourcesList(WebSocketSession session, JsonNode request) throws IOException {
        // 리소스 목록 반환 (예: 데이터베이스 테이블, 파일 등)
        var response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", request.get("id").asText());
        
        var result = objectMapper.createObjectNode();
        var resources = objectMapper.createArrayNode();
        
        // 예시: 화장실 데이터 리소스
        var toiletResource = objectMapper.createObjectNode();
        toiletResource.put("uri", "toilet://all");
        toiletResource.put("name", "화장실 데이터");
        toiletResource.put("description", "등록된 모든 화장실 정보");
        toiletResource.put("mimeType", "application/json");
        resources.add(toiletResource);
        
        result.set("resources", resources);
        response.set("result", result);
        sendMessage(session, response);
    }
    
    private void handleResourceRead(WebSocketSession session, JsonNode request) throws IOException {
        var params = request.get("params");
        String uri = params.get("uri").asText();
        
        var response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", request.get("id").asText());
        
        var result = objectMapper.createObjectNode();
        var contents = objectMapper.createArrayNode();
        
        // URI에 따라 적절한 리소스 반환
        if (uri.equals("toilet://all")) {
            var content = objectMapper.createObjectNode();
            content.put("uri", uri);
            content.put("mimeType", "application/json");
            content.put("text", "{}"); // 실제로는 화장실 데이터를 JSON으로 반환
            contents.add(content);
        }
        
        result.set("contents", contents);
        response.set("result", result);
        sendMessage(session, response);
    }
    
    private void sendErrorResponse(WebSocketSession session, JsonNode id, String message) throws IOException {
        var response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        }
        
        var error = objectMapper.createObjectNode();
        error.put("code", -1);
        error.put("message", message);
        response.set("error", error);
        
        sendMessage(session, response);
    }
    
    private void sendMessage(WebSocketSession session, JsonNode message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }
    
    private JsonNode createInitializeResponse() {
        var response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("method", "notifications/initialized");
        return response;
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("MCP Client disconnected: " + session.getId());
    }
}