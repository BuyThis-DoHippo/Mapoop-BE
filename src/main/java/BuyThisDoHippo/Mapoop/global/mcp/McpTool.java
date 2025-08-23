package BuyThisDoHippo.Mapoop.global.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP 도구 인터페이스
 */
public interface McpTool {
    
    /**
     * 도구의 이름을 반환합니다.
     */
    String getName();
    
    /**
     * 도구의 설명을 반환합니다.
     */
    String getDescription();
    
    /**
     * 도구의 입력 스키마를 반환합니다 (JSON Schema 형식).
     */
    JsonNode getInputSchema();
    
    /**
     * 도구를 실행하고 결과를 반환합니다.
     */
    Object execute(JsonNode arguments) throws Exception;
}