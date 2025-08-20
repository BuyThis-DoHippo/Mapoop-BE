package BuyThisDoHippo.Mapoop.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 도구 실행 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResult {
    private boolean success;
    private Object result;
    private String error;
}
