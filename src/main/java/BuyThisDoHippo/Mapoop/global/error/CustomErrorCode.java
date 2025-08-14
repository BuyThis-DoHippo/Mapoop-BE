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
    DATABASE_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1003,"데이터베이스 접근 중 오류가 발생했습니다."),

    // 2000: 유저 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2000, "사용자를 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 2001, "유효하지 않은 토큰입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, 2002, "인증이 필요합니다."),

    // 3000: 화장실 에러
    TOILET_NOT_FOUND(HttpStatus.NOT_FOUND, 3000, "화장실을 찾을 수 없습니다."),
    INVALID_TOILET_TYPE(HttpStatus.BAD_REQUEST, 3001, "유효하지 않은 화장실 타입입니다."),
    INVALID_GENDER_TYPE(HttpStatus.BAD_REQUEST, 3002, "유효하지 않은 성별 타입입니다."),
    GEOCODING_FAILED(HttpStatus.BAD_REQUEST, 3003, "주소를 좌표로 변환할 수 없습니다."),
    TOILET_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 3004, "화장실 등록에 실패했습니다."),

    // 4000: Redis 에러
    REDIS_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 4000, "Redis 연결에 실패했습니다."),
    REDIS_OPERATION_FAILED(HttpStatus.BAD_GATEWAY, 4001, "Redis 작업에 실패했습니다." );

    private final HttpStatus status;
    private final Integer code;
    private final String message;
}
