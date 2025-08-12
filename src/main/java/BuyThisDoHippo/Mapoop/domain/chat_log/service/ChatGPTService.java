package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class ChatGPTService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model:gpt-5-mini}")  // ← 설정에서 가져오기
    private String model;

    public ChatGPTService() {
    }

    /**
     * GPT API로 챗봇 답변 생성
     */
    public String generateChatResponse(String question, User user) {
        try {
            log.info("GPT API 호출 시작 - 질문: {}", question);

            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(30));

            String systemPrompt = buildSystemPrompt();

            String userPrompt = buildUserPrompt(question, user);

            List<ChatMessage> messages = List.of(
                    new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                    new ChatMessage(ChatMessageRole.USER.value(), userPrompt)
            );

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4o-mini")
                    .messages(messages)
                    .maxTokens(200)
                    .temperature(0.7)
                    .build();

            String response = service.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            log.info("GPT API 응답 성공");
            return response.trim();
        } catch (Exception e) {
            log.error("GPT API 호출 실패: {}", e.getMessage());
            return getFallbackResponse(question);
        }
    }

    private String buildSystemPrompt() {
        return """
            당신은 '마푸프(Mapoop)' 앱의 화장실 찾기 도우미입니다.
            
            역할:
            - 사용자가 화장실을 찾는 것을 도와주는 친근한 AI 어시스턴트
            - 화장실 위치, 청결도, 이용시간, 접근성 등에 대해 도움을 제공
            
            응답 스타일:
            - 친근하고 도움이 되는 톤으로 대화
            - 구체적이고 실용적인 정보 제공
            - 한국어로 응답
            - 150자 이내로 간결하게 답변
            - 이모지는 사용하지 마세요
            
            주요 기능:
            1. 근처 화장실 찾기 및 안내
            2. 24시간 이용 가능한 화장실 정보
            3. 청결한 화장실 추천
            4. 장애인 접근 가능한 화장실 안내
            5. 화장실 이용 팁 및 매너 안내
            """;
    }

    private String buildUserPrompt(String question, User user) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자 질문: ").append(question);

        if(user!=null) {
            prompt.append("\n\n사용자 정보: ");
            if(user.getIsLocationConsent()) {
                prompt.append("\n - 위치 정보 이용 동의: 예");
            }

            prompt.append("\n - 사용자명: ").append(user.getName());
        }

        return prompt.toString();
    }

    private String getFallbackResponse(String question) {
        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("가까운") || lowerQuestion.contains("근처")) {
            return "현재 위치에서 가장 가까운 화장실을 찾아드리겠습니다. 위치 정보를 허용해주시면 더 정확한 안내가 가능합니다.";
        }

        return "죄송합니다. 일시적인 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    }
}
