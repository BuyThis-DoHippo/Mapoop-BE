package BuyThisDoHippo.Mapoop.global.error;

import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
}
