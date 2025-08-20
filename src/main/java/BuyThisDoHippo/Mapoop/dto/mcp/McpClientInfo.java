package BuyThisDoHippo.Mapoop.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 클라이언트 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpClientInfo {
    private String name;
    private String version;
}
