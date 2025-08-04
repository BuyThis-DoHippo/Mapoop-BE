package BuyThisDoHippo.Mapoop.global.common;

import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class CommonResponse<T> {
    private int statusCode;
    private String message;
    private T data;

    public static <T> CommonResponse<T> onSuccess(T data, String message) {
        return new CommonResponse<>(HttpStatus.OK.value(), message, data);
    }

    public static <T> CommonResponse<T> onFailure(T data, CustomErrorCode errorCode) {
        return new CommonResponse<>(errorCode.getStatus().value(), errorCode.getMessage(), data);
    }
}
