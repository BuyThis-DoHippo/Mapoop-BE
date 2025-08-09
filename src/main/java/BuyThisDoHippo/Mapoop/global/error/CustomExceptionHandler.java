package BuyThisDoHippo.Mapoop.global.error;

import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice(annotations = RestController.class)
public class CustomExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonResponse<Void>> handleCustomException(ApplicationException e) {
        CommonResponse<Void> response = CommonResponse.onFailure(null, e.getErrorCode());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(response);
    }

    // Validation 핸들러 추가
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<String> errors = fieldErrors.stream()
                .map(error -> String.format("'%s': '%s'",error.getField(),error.getDefaultMessage()))
                .toList();

        CommonResponse<?> response = CommonResponse.onFailure(errors, CustomErrorCode.INVALID_REQUEST_DTO);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<?>> handleMissingParam(MissingServletRequestParameterException ex) {

        CommonResponse<?> response = CommonResponse.onFailure(null, CustomErrorCode.MISSING_REQUIRED_PARAM);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Redis 연결 실패
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<CommonResponse<?>> handleRedisConnectionFail(RedisConnectionFailureException ex) {
        CommonResponse<?> response = CommonResponse.onFailure(null, CustomErrorCode.REDIS_CONNECTION_FAILED);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Redis 명령/시스템 예외
    @ExceptionHandler({RedisSystemException.class, DataAccessException.class})
    public ResponseEntity<CommonResponse<?>> handleRedisSystemError(Exception ex) {
        CommonResponse<?> response = CommonResponse.onFailure(null, CustomErrorCode.REDIS_OPERATION_FAILED);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
