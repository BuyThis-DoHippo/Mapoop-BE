package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatGPTService {

    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @PostConstruct
    private void init() {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            log.error("OpenAI API 키가 설정되지 않았습니다!");
        } else {
            log.info("OpenAI API 키 로드됨 - 길이: {}, 시작: {}", 
                openaiApiKey.length(), 
                openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())));
        }
    }

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    public String recommendFromCandidates(
            String question,
            List<KakaoLocalService.PlaceDto> candidates,
            int maxPicks
    ) {
        // 후보를 JSON-like 문자열로 직렬화 (LLM에 넘길 재료)
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < candidates.size(); i++) {
            var c = candidates.get(i);
            if (i > 0) json.append(',');
            json.append("{")
                    .append("\"name\":").append(q(c.getName())).append(',')
                    .append("\"address\":").append(q(nz(c.getRoadAddress(), "주소 정보 없음"))).append(',')
                    .append("\"floor\":").append(q(nz(c.getBuildingFloor(), ""))).append(',')
                    .append("\"walkTime\":").append(q(nz(c.getWalkTime(), ""))).append(',')
                    .append("\"hours\":").append(q(nz(c.getHours(), "")))
                    .append("}");
        }
        json.append("]");

        String systemPrompt = String.format("""
            너는 친근하고 도움이 되는 화장실 추천 AI야.
            
            중요한 규칙:
            1. 반드시 내가 주는 candidates 배열 안에서만 선택해
            2. 사용자의 구체적인 요구사항을 잘 파악해서 맞춤 추천을 해줘
            3. 각 추천마다 왜 추천하는지 이유를 간단히 설명해줘
            4. 최대 %d개까지 추천해줘
            5. 친근하고 자연스러운 말투로 답해줘
            
            출력 형식:
            • [화장실명] - [간단한 추천 이유]
              📍 [주소] | 🚶‍♂️ [도보시간] | 🏢 [층수정보]
            
            질문의 특성에 따라 답변 스타일을 조정해:
            - 급한 상황: 가장 가까운 곳 우선
            - 깨끗함 요구: 평점이나 청결 관련 정보 강조  
            - 접근성 요구: 장애인 시설이나 접근 편의성 강조
            """, maxPicks);

        String userPrompt = String.format("""
            사용자 질문: "%s"
            
            현재 시각: %s
            
            추천 후보 화장실들:
            %s
            
            위 후보들 중에서 사용자의 질문에 가장 잘 맞는 화장실을 골라 추천해줘.
            각각의 장단점과 추천 이유를 포함해서 답변해줘.
            """, question, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), json);

        try {
            log.info("OpenAI 요청 - 시스템 프롬프트: {}", systemPrompt);
            log.info("OpenAI 요청 - 사용자 프롬프트: {}", userPrompt);
            
            String reply = callOpenAI(systemPrompt, userPrompt);
            
            if (reply != null && !reply.isBlank()) {
                log.info("OpenAI API 응답 성공 - 질문: '{}', 응답: '{}'", question, reply);
                return reply;
            } else {
                log.warn("OpenAI API 응답이 비어있음 - 폴백 사용");
                return fallbackFormat(candidates, maxPicks);
            }
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패 - 폴백 사용: ", e);
            return fallbackFormat(candidates, maxPicks);
        }
    }

    private String callOpenAI(String systemPrompt, String userPrompt) {
        try {
            // API 키 디버깅
            log.info("OpenAI API 키 길이: {}, 시작 문자: {}", 
                openaiApiKey != null ? openaiApiKey.length() : "null",
                openaiApiKey != null ? openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())) : "null"
            );
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 500,
                "temperature", 0.9,  // ← 더 다양한 답변을 위해 증가
                "top_p", 0.95,       // ← 추가: 더 창의적인 답변
                "presence_penalty", 0.3,  // ← 추가: 반복 줄이기
                "frequency_penalty", 0.3  // ← 추가: 빈번한 단어 줄이기
            );

            log.info("OpenAI 요청 본문: {}", requestBody);

            Mono<Map> response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                        log.error("OpenAI API 오류 - 상태: {}, 헤더: {}", 
                            clientResponse.statusCode(), clientResponse.headers().asHttpHeaders());
                        return clientResponse.bodyToMono(String.class)
                            .map(body -> {
                                log.error("OpenAI API 오류 응답: {}", body);
                                return new RuntimeException("OpenAI API Error: " + clientResponse.statusCode() + " - " + body);
                            });
                    })
                    .bodyToMono(Map.class);

            Map<String, Object> result = response.block();
            
            if (result != null && result.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류: ", e);
            throw e;
        }
    }

    private static String fallbackFormat(List<KakaoLocalService.PlaceDto> cs, int n) {
        var sb = new StringBuilder("조건에 맞는 추천입니다:\n");
        for (int i = 0; i < Math.min(cs.size(), n); i++) {
            var c = cs.get(i);
            sb.append("• ")
                    .append(c.getName())
                    .append(" (").append(nz(c.getWalkTime(), "도보 약 ?분")).append(", ")
                    .append(nz(c.getBuildingFloor(), "")).append(")")
                    .append(" — ").append(nz(c.getRoadAddress(), "주소 정보 없음"))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String q(String s) { 
        return "\"" + (s == null ? "" : s.replace("\"", "\\\"")) + "\""; 
    }
    
    private static String nz(String s, String d) { 
        return (s == null || s.isBlank()) ? d : s; 
    }
}
