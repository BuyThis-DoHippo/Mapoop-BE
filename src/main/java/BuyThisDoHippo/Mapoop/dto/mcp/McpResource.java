package BuyThisDoHippo.Mapoop.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 리소스 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpResource {
    private String uri;
    private String name;
    private String description;
    private String mimeType;
}
