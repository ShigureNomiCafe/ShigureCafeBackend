package cafe.shigure.ShigureCafeBackend.exception;

import cafe.shigure.ShigureCafeBackend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        if ("RATE_LIMIT_EXCEEDED".equals(e.getCode())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ErrorResponse.of(e.getCode(), e.getMetadata()));
        }
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMetadata()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("ACCOUNT_DISABLED"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("AUTHENTICATION_FAILED"));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String field = ex.getBindingResult().getFieldError().getField();
        String defaultMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_FAILED", 
                Map.of("field", field, "reason", defaultMessage)));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e) {
        e.printStackTrace();
        
        String code = "INTERNAL_SERVER_ERROR";
        if (e instanceof org.springframework.dao.DataIntegrityViolationException) {
            code = "DATA_INTEGRITY_VIOLATION";
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(code));
    }
}
