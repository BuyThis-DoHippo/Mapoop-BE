package BuyThisDoHippo.Mapoop.domain.chat_log.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot/test")
@Slf4j
public class ChatBotTestController {

    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    /**
     * OpenAI API 연결 테스트
     */
    @GetMapping("/openai")
    public Map<String, Object> testOpenAI() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // API 키 상태 확인
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                result.put("status", "FAIL");
                result.put("message", "OpenAI API 키가 설정되지 않음");
                return result;
            }
            
            result.put("apiKeyStatus", "설정됨");
            result.put("apiKeyLength", openaiApiKey.length());
            result.put("apiKeyPrefix", openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())));
            result.put("model", model);
            
            // 간단한 API 테스트
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", java.util.List.of(
                    Map.of("role", "user", "content", "Hello, this is a test.")
                ),
                "max_tokens", 50
            );
            
            Mono<Map> response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                        log.error("OpenAI API 테스트 실패 - 상태: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                            .map(body -> {
                                log.error("오류 응답: {}", body);
                                return new RuntimeException("API Test Failed: " + clientResponse.statusCode());
                            });
                    })
                    .bodyToMono(Map.class);
            
            Map<String, Object> apiResult = response.block();
            
            if (apiResult != null && apiResult.containsKey("choices")) {
                result.put("status", "SUCCESS");
                result.put("message", "OpenAI API 연결 성공");
                result.put("responseReceived", true);
            } else {
                result.put("status", "FAIL");
                result.put("message", "OpenAI API 응답 형식 오류");
                result.put("apiResponse", apiResult);
            }
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "OpenAI API 테스트 중 오류: " + e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            log.error("OpenAI API 테스트 실패", e);
        }
        
        return result;
    }
    
    /**
     * 환경 변수 확인
     */
    @GetMapping("/env")
    public Map<String, Object> checkEnvironment() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("openaiApiKey", openaiApiKey != null ? 
            openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())) + "..." : "null");
        result.put("openaiModel", model);
        result.put("systemEnvCheck", System.getenv("OPENAI_API_KEY") != null ? "설정됨" : "없음");
        
        return result;
    }
}
