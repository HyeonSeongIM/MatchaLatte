package project.matchalatte.core.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import project.matchalatte.core.support.error.CoreException;
import project.matchalatte.core.support.error.ErrorType;
import project.matchalatte.core.support.error.UserException;
import project.matchalatte.core.support.response.ApiResponse;
import project.matchalatte.support.logging.TraceIdContext;

import java.util.NoSuchElementException;

import static project.matchalatte.core.support.error.ErrorType.DEFAULT_ERROR;

@RestControllerAdvice
public class ApiControllerAdvice {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<?>> handleCoreException(CoreException e) {
        switch (e.getErrorType().getLogLevel()) {
            case ERROR -> log.error("CoreException : {}", e.getMessage(), e);
            case WARN -> log.warn("CoreException : {}", e.getMessage(), e);
            default -> log.info("CoreException : {}", e.getMessage(), e);
        }
        return new ResponseEntity<>(ApiResponse.error(e.getErrorType(), e.getData()), e.getErrorType().getStatus());
    }

    // TODO: 이거 레거시 코드 삭제하기
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<?>> handleUserException(UserException e) {
        String traceId = TraceIdContext.traceId();
        switch (e.getErrorType().getLogLevel()) {
            case ERROR -> log.error("{} | UserException : {}", traceId, e.getMessage(), e);
            case WARN -> log.warn("{} | UserException : {}", traceId, e.getMessage(), e);
            default -> log.info("{} | UserException : {}", traceId, e.getMessage(), e);
        }
        return new ResponseEntity<>(ApiResponse.error(e.getErrorType(), e.getData()), e.getErrorType().getStatus());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<?>> handleNoSuchElement(NoSuchElementException e) {
        RuntimeException wrapped = wrapWithTraceId(e, "NoSuchElementException : ");
        log.warn(wrapped.getMessage(), wrapped);

        return new ResponseEntity<>(ApiResponse.error(ErrorType.DEFAULT_ERROR, "리소스를 찾을 수 없습니다."),
                ErrorType.DEFAULT_ERROR.getStatus());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<?>> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.warn("Validation Exception : {}", errorMessage);

        return new ResponseEntity<>(ApiResponse.error(ErrorType.VALID_ERROR, errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Exception : {}", e.getMessage(), e);
        return new ResponseEntity<>(ApiResponse.error(DEFAULT_ERROR), DEFAULT_ERROR.getStatus());
    }

    private RuntimeException wrapWithTraceId(Throwable cause, String title) {
        String tid = TraceIdContext.traceId();
        String msg = (cause.getMessage() == null) ? title : (title + cause.getMessage());
        return new RuntimeException(tid + " | " + msg, cause);
    }

}
