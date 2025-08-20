package BuyThisDoHippo.Mapoop.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 기능 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpCapabilities {
    private boolean tools;
    private boolean resources;
    private boolean prompts;
    private boolean logging;
}
