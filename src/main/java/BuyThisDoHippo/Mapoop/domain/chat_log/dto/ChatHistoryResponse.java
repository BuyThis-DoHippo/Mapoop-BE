package BuyThisDoHippo.Mapoop.domain.chat_log.dto;


import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class ChatHistoryResponse {

    private List<ChatResponse> conversations;
    private PaginationInfo pagination;

    @Getter
    @Builder
    public static class PaginationInfo {
        private int page;
        private int size;
        private long total;
        private int totalPages;
    }

    public static ChatHistoryResponse from(Page<ChatResponse> chatPage) {
        return ChatHistoryResponse.builder()
                .conversations(chatPage.getContent())
                .pagination(PaginationInfo.builder()
                        .page(chatPage.getNumber() + 1)
                        .size(chatPage.getSize())
                        .total(chatPage.getTotalElements())
                        .totalPages(chatPage.getTotalPages())
                        .build())
                .build();
    }
}
