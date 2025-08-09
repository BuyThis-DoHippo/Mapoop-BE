package BuyThisDoHippo.Mapoop.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
    private String kakaoAccessToken;
    private Boolean locationConsent;
    private String locationConsentVersion;
}
