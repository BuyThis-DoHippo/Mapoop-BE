package BuyThisDoHippo.Mapoop.domain.chat_log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatAskRequest {

    @NotBlank(message = "질문을 입력해주세요")
    @Size(max = 500, message = "질문은 500자 이내로 입력해주세요")
    private String question;

    public ChatAskRequest(String question) {
        this.question = question;
    }
}
