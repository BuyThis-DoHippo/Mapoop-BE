package BuyThisDoHippo.Mapoop.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationException extends RuntimeException {
    CustomErrorCode errorCode;
}
