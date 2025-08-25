package BuyThisDoHippo.Mapoop.domain.user.dto;

import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String kakaoId;
    private Boolean locationConsent;
    private LocalDateTime locationConsentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .kakaoId(user.getKakaoId())
                .locationConsent(user.getIsLocationConsent())
                .locationConsentDate(user.getLocationConsentDate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}