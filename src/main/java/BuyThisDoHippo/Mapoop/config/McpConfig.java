package BuyThisDoHippo.Mapoop.config;

import BuyThisDoHippo.Mapoop.service.McpClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * MCP (Model Context Protocol) 설정 클래스
 */
@Configuration
@EnableWebSocket
public class McpConfig implements WebSocketConfigurer {

    @Value("${mcp.server.url:http://localhost:3000}")
    private String mcpServerUrl;

    @Value("${mcp.enabled:true}")
    private boolean mcpEnabled;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (mcpEnabled) {
            registry.addHandler(new McpWebSocketHandler(), "/mcp-ws")
                   .setAllowedOrigins("*");
        }
    }

    @Bean
    public McpClientService mcpClientService() {
        return new McpClientService(mcpServerUrl, mcpEnabled);
    }
}
