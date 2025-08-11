package BuyThisDoHippo.Mapoop.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomErrorCode {

    // 1000: 일반 에러
    FORBIDDEN(HttpStatus.FORBIDDEN, 1000, "금지된 요청입니다."),
    INVALID_REQUEST_DTO(HttpStatus.BAD_REQUEST, 1001, "요청 데이터가 조건에 만족하지 않습니다."),
    MISSING_REQUIRED_PARAM(HttpStatus.BAD_REQUEST, 1002, "필수 파라미터를 누락했습니다."),
    DATABASE_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1003, "데이터베이스 접근 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1004, "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, 1005, "권한이 없습니다."),

    // 2000: 유저 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "사용자를 찾을 수 없습니다."),

    // 4000: Redis 에러
    REDIS_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 4000, "Redis 연결에 실패했습니다."),
    REDIS_OPERATION_FAILED(HttpStatus.BAD_GATEWAY, 4001, "Redis 작업에 실패했습니다."),

    // 4100: 채팅 에러
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, 4101, "대화를 찾을 수 없습니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, 4102, "해당 대화에 접근할 권한이 없습니다.");

    private final HttpStatus status;
    private final Integer code;
    private final String message;
}
