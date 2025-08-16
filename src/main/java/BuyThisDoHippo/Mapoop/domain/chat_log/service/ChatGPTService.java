package BuyThisDoHippo.Mapoop.domain.chat_log.service;

import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatGPTService {

    // 실제 OpenAI/LLM 클라이언트 주입부는 프로젝트에 맞게
    // private final OpenAiClient openAi;

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

        String system = """
            너는 화장실 추천 도우미야.
            반드시 내가 주는 candidates 배열 안에서만 골라. 후보 밖 장소를 만들거나 추측하지 마.
            사용자의 요구(예: 별점/접근성/가까움 등)를 고려해서 최대 N개를 추천하고,
            각 추천마다 한 줄 설명을 붙여 bullet로 출력해.
            """;

        String user = """
            질문: %s
            N: %d
            candidates: %s
            출력 예:
            • 이름 (도보 약 X분, 층수/특징) — 주소
            • ...
            """.formatted(question, maxPicks, json);

        // 실제 LLM 호출부 (프로젝트 클라이언트에 맞게 교체)
        // String reply = openAi.chat(system, user);

        // 데모/폴백 포맷터 (LLM 연결 전이라도 동작하도록)
        String reply = fallbackFormat(candidates, maxPicks);

        return (reply == null || reply.isBlank())
                ? "추천을 생성하지 못했어요. 잠시 후 다시 시도해주세요."
                : reply;
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
