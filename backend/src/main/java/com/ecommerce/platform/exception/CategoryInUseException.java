package com.ecommerce.platform.exception;

                                                                                               
public class CategoryInUseException extends ApiException {
    public CategoryInUseException(Long categoryId) {
        super(ErrorCode.CATEGORY_IN_USE,
                "Category " + categoryId + " still has active products referencing it");
    }
}
