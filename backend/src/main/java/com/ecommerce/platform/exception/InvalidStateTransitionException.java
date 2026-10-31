package com.ecommerce.platform.exception;

                                                                                            
public class InvalidStateTransitionException extends ApiException {
    public InvalidStateTransitionException(String from, String to) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                "Invalid order status transition: " + from + " -> " + to);
    }
}
