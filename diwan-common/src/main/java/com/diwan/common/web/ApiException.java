package com.diwan.common.web;

import org.springframework.http.HttpStatus;

/**
 * An expected error with a message key instead of a fixed text. {@link GlobalExceptionHandler} resolves the
 * key in the request's language ({@code Accept-Language}, from the service's {@code messages*.properties})
 * and returns it as {@code {"success":false,"code":"<key>","message":"<localized text>"}}, so clients can
 * branch on {@code code} and show {@code message}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient Object[] args;

    public ApiException(HttpStatus status, String code, Object... args) {
        super(code);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public static ApiException notFound(String code, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, code, args);
    }

    public static ApiException badRequest(String code, Object... args) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, args);
    }

    public static ApiException conflict(String code, Object... args) {
        return new ApiException(HttpStatus.CONFLICT, code, args);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public Object[] getArgs() { return args; }
}
