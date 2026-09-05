package com.power.middleware.exception;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.result.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ExceptionProperties exceptionProperties;

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException ex) {
        log.warn("BizException code={}, message={}", ex.getCode(), ex.getMessage());
        return withSource(R.fail(ex.getCode(), ex.getMessage()), ex);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public R<Void> handleAccessDenied(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return R.fail(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException manv) {
            message = manv.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        } else {
            BindException be = (BindException) ex;
            message = be.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        log.warn("Validation failed: {}", message);
        return R.fail(ErrorCode.VALIDATION_FAILED.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", message);
        return R.fail(ErrorCode.VALIDATION_FAILED.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Bad request body: {}", ex.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        return withSource(R.fail(ErrorCode.SYSTEM_ERROR), ex);
    }

    private R<Void> withSource(R<Void> result, Throwable ex) {
        if (!exceptionProperties.isIncludeSource()) {
            return result;
        }
        for (StackTraceElement element : ex.getStackTrace()) {
            if (element.getClassName().startsWith("com.power.")) {
                return result.source(element.getClassName() + "#" + element.getMethodName()
                        + ":" + element.getLineNumber());
            }
        }
        return result.source(ex.getClass().getName());
    }
}
