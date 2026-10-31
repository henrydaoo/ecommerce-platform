package com.ecommerce.platform.exception;

                                                                                                 
public class OptimisticLockConflictException extends ApiException {
    public OptimisticLockConflictException(String resourceName, Object identifier) {
        super(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                resourceName + " " + identifier + " was modified concurrently - please retry");
    }
}
