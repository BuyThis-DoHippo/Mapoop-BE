package BuyThisDoHippo.Mapoop.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {
    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private Object params;
    private Object result;
    private McpError error;
}
