package com.ecommerce.platform.exception;

   
                                                                            
                                                                            
   
public class InsufficientStockException extends ApiException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "Insufficient stock for product " + productId
                        + ": requested " + requested + ", available " + available);
    }
}
