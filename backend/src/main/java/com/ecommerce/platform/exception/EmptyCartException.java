package com.ecommerce.platform.exception;

                                                                   
public class EmptyCartException extends ApiException {
    public EmptyCartException() {
        super(ErrorCode.EMPTY_CART, "Cannot checkout: cart is empty");
    }
}
