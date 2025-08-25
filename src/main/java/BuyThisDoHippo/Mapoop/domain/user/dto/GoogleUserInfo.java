package BuyThisDoHippo.Mapoop.domain.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GoogleUserInfo {
    private String sub;
    private String name;
    private String email;
    private Boolean email_verified;
}
