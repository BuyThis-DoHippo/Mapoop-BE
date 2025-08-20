package BuyThisDoHippo.Mapoop.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 에러 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpError {
    private int code;
    private String message;
    private Object data;
}
