package com.ecommerce.platform.service;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.request.CreateProductRequest;
import com.ecommerce.platform.dto.request.UpdateProductRequest;
import com.ecommerce.platform.dto.response.ProductResponse;
import com.ecommerce.platform.entity.Product;

public interface ProductService {

    PageResponse<ProductResponse> list(Long categoryId, String search, int page, int size);

    PageResponse<ProductResponse> listAllForAdmin(int page, int size);

    ProductResponse getById(Long id);

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(Long id, UpdateProductRequest request);

                                                                                 
    void delete(Long id);

    void activate(Long id);

       
                                                                              
                                                                            
                                                                          
                                                                              
                                                                            
                                       
       
    Product decrementStockForCheckout(Long productId, int quantity);

       
                                                                                 
            
       
    void restoreStockForCancelledOrder(Long productId, int quantity);
}
