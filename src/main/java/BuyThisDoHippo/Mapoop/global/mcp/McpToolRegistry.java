package BuyThisDoHippo.Mapoop.global.mcp;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 도구들을 등록하고 관리하는 레지스트리
 */
@Component
public class McpToolRegistry {
    
    private final Map<String, McpTool> tools = new HashMap<>();
    
    /**
     * 도구를 등록합니다.
     */
    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }
    
    /**
     * 이름으로 도구를 조회합니다.
     */
    public McpTool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * 등