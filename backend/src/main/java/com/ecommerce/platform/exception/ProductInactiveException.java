package com.ecommerce.platform.exception;

                                                                                                 
public class ProductInactiveException extends ApiException {
    public ProductInactiveException(Long productId) {
        super(ErrorCode.PRODUCT_INACTIVE, "Product " + productId + " is no longer available");
    }
}
