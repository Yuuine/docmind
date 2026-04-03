package yuuine.docmind.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yuuine.docmind.common.model.Result;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.warn("[BusinessException] traceId: {}, path: {}, code: {}, message: {}",
                traceId, request.getRequestURI(), e.getErrorCode().getHttpCode(), e.getMessage());
        Result<Void> result = Result.error(e.getErrorCode().getHttpCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.valueOf(e.getErrorCode().getHttpCode())).body(result);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[ValidationException] traceId: {}, path: {}, errors: {}",
                traceId, request.getRequestURI(), errors);
        return Result.error(400, "参数校验失败: " + errors);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[BindException] traceId: {}, path: {}, errors: {}",
                traceId, request.getRequestURI(), errors);
        return Result.error(400, "参数绑定失败: " + errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.warn("[IllegalArgumentException] traceId: {}, path: {}, message: {}",
                traceId, request.getRequestURI(), e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.error("[Exception] traceId: {}, path: {}, message: {}",
                traceId, request.getRequestURI(), e.getMessage(), e);
        return Result.error(500, "系统内部错误，请稍后重试");
    }
}
