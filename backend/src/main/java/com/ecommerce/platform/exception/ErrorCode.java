package com.ecommerce.platform.exception;

import org.springframework.http.HttpStatus;

   
                                                                       
                                                                             
   
public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT),
    INVALID_STATE_TRANSITION(HttpStatus.BAD_REQUEST),
    CATEGORY_IN_USE(HttpStatus.CONFLICT),
    PRODUCT_INACTIVE(HttpStatus.CONFLICT),
    EMPTY_CART(HttpStatus.BAD_REQUEST),
    PAYMENT_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST),
    PAYMENT_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED_OR_REVOKED(HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
