package org.example.discountcode.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public BusinessException(ErrorCode errorCode, HttpStatus status) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.status = status;
    }

    public BusinessException(ErrorCode errorCode, HttpStatus status, Throwable cause) {
        super(errorCode.defaultMessage(), cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public HttpStatus status() {
        return status;
    }
}
