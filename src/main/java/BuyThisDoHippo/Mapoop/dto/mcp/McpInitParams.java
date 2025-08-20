package BuyThisDoHippo.Mapoop.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 초기화 파라미터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpInitParams {
    private String protocolVersion;
    private McpCapabilities capabilities;
    private McpClientInfo clientInfo;
}
