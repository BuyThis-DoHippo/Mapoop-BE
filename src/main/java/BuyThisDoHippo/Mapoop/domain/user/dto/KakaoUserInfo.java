package BuyThisDoHippo.Mapoop.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfo {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    private Properties properties;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {
        private String email;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        @JsonProperty("has_email")
        private Boolean hasEmail;

        @JsonProperty("is_email_valid")
        private Boolean isEmailValid;

        @JsonProperty("is_email_verified")
        private Boolean isEmailVerified;
    }

    @Getter
    @NoArgsConstructor
    public static class Properties {
        private String nickname;

        @JsonProperty("profile_image")
        private String profileImage;

        @JsonProperty("thumbnail_image")
        private String thumbnailImage;
    }

    // 편의 메서드들
    public String getEmail() {
        // 실제 이메일이 있으면 사용
        if (kakaoAccount != null && kakaoAccount.getEmail() != null) {
            return kakaoAccount.getEmail();
        }
        // 이메일이 없으면 카카오 ID로 가짜 이메일 생성
        return "kakao_" + getKakaoId() + "@mapoop.com";
    }
    public String getNickname() {
        return properties != null ? properties.getNickname() : null;
    }

    public String getKakaoId() {
        return id != null ? id.toString() : null;
    }
}
