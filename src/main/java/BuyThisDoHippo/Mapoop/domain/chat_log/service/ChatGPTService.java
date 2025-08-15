package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ChatGPTService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model:gpt-5-mini}")  // ← 설정에서 가져오기
    private String model;

    private final ObjectMapper om = new ObjectMapper();

    private OpenAIClient client;

    private OpenAIClient client() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public String generateChatResponse(String question, User user) {
        return generateChatResponse(question, user, null);
    }

    /**
     * GPT API로 챗봇 답변 생성
     */
    public String generateChatResponse(String question, User user, Object places) {
        try {
            log.info("GPT API 호출 시작 - 질문: {}", question);

            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(question, user, places);

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(model)
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .maxCompletionTokens(200)
                    .temperature(0.2)
                    .build();

            ChatCompletion res = client().chat().completions().create(params);

            var choices = res.choices();
            if (choices == null || choices.isEmpty()) {
                return getFallbackResponse(question);
            }

            var first = choices.get(0);
            return first.message().content()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .orElse(getFallbackResponse(question));
        } catch (Exception e) {
            log.error("GPT API 호출 실패", e);
            return getFallbackResponse(question);
        }
    }

    private String buildSystemPrompt() {
        return """
                너는 '마푸프' 화장실 안내 도우미.
                반드시 제공된 places[] 데이터만 사용해 답해. 데이터 없으면
                "정확한 위치 확인 후 알려드리겠습니다"라고만 답해.
                
                규칙:
                - 지하철역/공원 내 화장실을 최우선으로 선택
                - 아래 필드를 모두 채워서 150자 이내 한 문단으로 출력
                필드: {name, roadAddress, buildingFloor, walkTime, hours}
                
                출력형식(딱 한 줄):
                📍{name} ({roadAddress}) · {buildingFloor} · 🚶{walkTime} · 🕐{hours}
        """;
    }

    private String buildUserPrompt(String question, User user, Object places) {
        StringBuilder sb = new StringBuilder();
        sb.append("사용자 질문: ").append(question);

        if (user != null) {
            sb.append("\n\n사용자 정보:");
            if (Boolean.TRUE.equals(user.getIsLocationConsent())) {
                sb.append("\n - 위치 정보 이용 동의: 예");
            }
            if (user.getName() != null) {
                sb.append("\n - 사용자명: ").append(user.getName());
            }
        }

        if (places != null) {
            sb.append("\n\nplaces:\n").append(toJsonSafe(places));
        } else {
            sb.append("\n\nplaces: []");
        }
        return sb.toString();
    }

    private String toJsonSafe(Object o) {
        try {
            return om.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.warn("places 직렬화 실패: {}", e.getMessage());
            return "[]";
        }
    }

    private String getFallbackResponse(String question) {
        String lowerQuestion = question.toLowerCase();
        if (lowerQuestion.contains("가까운") || lowerQuestion.contains("근처")) {
            return "현재 위치에서 가장 가까운 화장실을 찾아드리겠습니다. 위치 정보를 허용해주시면 더 정확한 안내가 가능합니다.";
        }
        return "죄송합니다. 일시적인 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    }
}
