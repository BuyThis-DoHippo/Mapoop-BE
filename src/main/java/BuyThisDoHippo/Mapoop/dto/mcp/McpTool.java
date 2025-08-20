package BuyThisDoHippo.Mapoop.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 도구 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;
}
