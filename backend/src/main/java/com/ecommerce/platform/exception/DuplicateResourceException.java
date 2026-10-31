package com.ecommerce.platform.exception;

                                                                                                          
public class DuplicateResourceException extends ApiException {
    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
