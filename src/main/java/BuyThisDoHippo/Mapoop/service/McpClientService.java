package BuyThisDoHippo.Mapoop.service;

import BuyThisDoHippo.Mapoop.dto.mcp.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP 클라이언트 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpClientService {
    
    private final String mcpServerUrl;
    private final boolean mcpEnabled;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public McpClientService(String mcpServerUrl, boolean mcpEnabled) {
        this.mcpServerUrl = mcpServerUrl;
        this.mcpEnabled = mcpEnabled;
    }

    /**
     * MCP 서버에서 사용 가능한 도구 목록을 가져옵니다.
     */
    public List<McpTool> getAvailableTools() {
        if (!mcpEnabled) {
            log.warn("MCP가 비활성화되어 있습니다.");
            return List.of();
        }

        try {
            McpMessage request = McpMessage.builder()
                .id(UUID.randomUUID().toString())
                .method("tools/list")
                .params(Map.of())
                .build();

            McpMessage response = sendRequest(request);
            
            if (response.getError() != null) {
                log.error("MCP 도구 목록 조회 실패: {}", response.getError().getMessage());
                return List.of();
            }

            // 응답에서 도구 목록 추출
            Map<String, Object> result = (Map<String, Object>) response.getResult();
            List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
            
            return tools.stream()
                .map(this::mapToMcpTool)
                .toList();
                
        } catch (Exception e) {
            log.error("MCP 도구 목록 조회 중 오류 발생", e);
            return List.of();
        }
    }

    /**
     * MCP 도구를 호출합니다.
     */
    public McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        if (!mcpEnabled) {
            log.warn("MCP가 비활성화되어 있습니다.");
            return McpToolResult.builder()
                .success(false)
                .error("MCP가 비활성화되어 있습니다.")
                .build();
        }

        try {
            McpMessage request = McpMessage.builder()
                .id(UUID.randomUUID().toString())
                .method("tools/call")
                .params(Map.of(
                    "name", toolName,
                    "arguments", arguments
                ))
                .build();

            McpMessage response = sendRequest(request);
            
            if (response.getError() != null) {
                log.error("MCP 도구 호출 실패: {}", response.getError().getMessage());
                return McpToolResult.builder()
                    .success(false)
                    .error(response.getError().getMessage())
                    .build();
            }

            Map<String, Object> result = (Map<String, Object>) response.getResult();
            
            return McpToolResult.builder()
                .success(true)
                .result(result.get("content"))
                .build();
                
        } catch (Exception e) {
            log.error("MCP 도구 호출 중 오류 발생", e);
            return McpToolResult.builder()
                .success(false)
                .error("도구 호출 중 오류가 발생했습니다: " + e.getMessage())
                .build();
        }
    }

    /**
     * MCP 서버에서 사용 가능한 리소스 목록을 가져옵니다.
     */
    public List<McpResource> getAvailableResources() {
        if (!mcpEnabled) {
            log.warn("MCP가 비활성화되어 있습니다.");
            return List.of();
        }

        try {
            McpMessage request = McpMessage.builder()
                .id(UUID.randomUUID().toString())
                .method("resources/list")
                .params(Map.of())
                .build();

            McpMessage response = sendRequest(request);
            
            if (response.getError() != null) {
                log.error("MCP 리소스 목록 조회 실패: {}", response.getError().getMessage());
                return List.of();
            }

            Map<String, Object> result = (Map<String, Object>) response.getResult();
            List<Map<String, Object>> resources = (List<Map<String, Object>>) result.get("resources");
            
            return resources.stream()
                .map(this::mapToMcpResource)
                .toList();
                
        } catch (Exception e) {
            log.error("MCP 리소스 목록 조회 중 오류 발생", e);
            return List.of();
        }
    }

    /**
     * MCP 서버로 요청을 전송합니다.
     */
    private McpMessage sendRequest(McpMessage request) throws Exception {
        HttpPost httpPost = new HttpPost(mcpServerUrl + "/mcp");
        
        String jsonRequest = objectMapper.writeValueAsString(request);
        httpPost.setEntity(new StringEntity(jsonRequest, "UTF-8"));
        httpPost.setHeader("Content-Type", "application/json");

        return httpClient.execute(httpPost, response -> {
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            return objectMapper.readValue(responseBody, McpMessage.class);
        });
    }

    private McpTool mapToMcpTool(Map<String, Object> toolData) {
        return McpTool.builder()
            .name((String) toolData.get("name"))
            .description((String) toolData.get("description"))
            .inputSchema((Map<String, Object>) toolData.get("inputSchema"))
            .build();
    }

    private McpResource mapToMcpResource(Map<String, Object> resourceData) {
        return McpResource.builder()
            .uri((String) resourceData.get("uri"))
            .name((String) resourceData.get("name"))
            .description((String) resourceData.get("description"))
            .mimeType((String) resourceData.get("mimeType"))
            .build();
    }
}
