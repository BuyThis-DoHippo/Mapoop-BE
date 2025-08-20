package BuyThisDoHippo.Mapoop.controller;

import BuyThisDoHippo.Mapoop.dto.mcp.McpResource;
import BuyThisDoHippo.Mapoop.dto.mcp.McpTool;
import BuyThisDoHippo.Mapoop.dto.mcp.McpToolResult;
import BuyThisDoHippo.Mapoop.service.McpClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpClientService mcpClientService;

    /**
     * MCP 서버에서 사용 가능한 도구 목록을 조회합니다.
     */
    @GetMapping("/tools")
    public ResponseEntity<List<McpTool>> getTools() {
        try {
            List<McpTool> tools = mcpClientService.getAvailableTools();
            return ResponseEntity.ok(tools);
        } catch (Exception e) {
            log.error("MCP 도구 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * MCP 도구를 호출합니다.
     */
    @PostMapping("/tools/{toolName}/call")
    public ResponseEntity<McpToolResult> callTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments) {
        try {
            McpToolResult result = mcpClientService.callTool(toolName, arguments);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("MCP 도구 호출 실패: {}", toolName, e);
            return ResponseEntity.internalServerError()
                .body(McpToolResult.builder()
                    .success(false)
                    .error("도구 호출 중 오류가 발생했습니다: " + e.getMessage())
                    .build());
        }
    }

    /**
     * MCP 서버에서 사용 가능한 리소스 목록을 조회합니다.
     */
    @GetMapping("/resources")
    public ResponseEntity<List<McpResource>> getResources() {
        try {
            List<McpResource> resources = mcpClientService.getAvailableResources();
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            log.error("MCP 리소스 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * MCP 연결 상태를 확인합니다.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            List<McpTool> tools = mcpClientService.getAvailableTools();
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "toolCount", tools.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("MCP 상태 확인 실패", e);
            return ResponseEntity.ok(Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
