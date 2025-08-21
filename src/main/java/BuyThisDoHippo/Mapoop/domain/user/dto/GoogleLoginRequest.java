package BuyThisDoHippo.Mapoop.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequest {
    private String googleAccessToken;
    private Boolean locationConsent;
    private String locationConsentVersion;
}
