package BuyThisDoHippo.Mapoop.domain.chat_log.dto;


import BuyThisDoHippo.Mapoop.domain.chat_log.entity.ChatLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatResponse {

    private Long id;
    private String question;
    private String answer;
    private LocalDateTime createdAt;

    public static ChatResponse from(ChatLog chatLog) {
        return ChatResponse.builder()
                .id(chatLog.getId())
                .question(chatLog.getQuestion())
                .answer(chatLog.getAnswer())
                .createdAt(chatLog.getCreatedAt())
                .build();
    }
}
