package com.diwan.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Uniform error body {@code {"success": false, "message": "..."}}.
 * Exceptions raised by Spring MVC itself keep their proper status (400/404/405/415...);
 * only unexpected exceptions become 500. {@link ApiException}s also carry a {@code code} and are translated
 * with the service's {@link MessageSource} into the request's language.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messages;

    public GlobalExceptionHandler() {
        this(null);
    }

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(ApiException ex) {
        String text = messages == null ? ex.getCode()
                : messages.getMessage(ex.getCode(), ex.getArgs(), ex.getCode(), LocaleContextHolder.getLocale());
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("success", false, "code", ex.getCode(), "message", text));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "code", "validation.failed", "message", errors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = ex instanceof ErrorResponse er && er.getBody().getDetail() != null
                ? er.getBody().getDetail() : ex.getMessage();
        return ResponseEntity.status(statusCode).headers(headers).body(body(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(ex.getMessage() != null ? ex.getMessage() : "Internal server error"));
    }

    private static Object body(String message) {
        return Map.of("success", false, "message", message != null ? message : "Error");
    }
}
