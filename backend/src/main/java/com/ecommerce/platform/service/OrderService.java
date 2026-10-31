package com.ecommerce.platform.service;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.request.CheckoutRequest;
import com.ecommerce.platform.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.platform.dto.response.OrderResponse;

public interface OrderService {

       
                                                                            
                                                                            
                                                                          
                                                            
       
    OrderResponse checkout(Long userId, String idempotencyKey, CheckoutRequest request);

    PageResponse<OrderResponse> listForUser(Long userId, int page, int size);

       
                                                                                   
                     
       
    OrderResponse getById(Long userId, boolean isAdmin, Long orderId);

    PageResponse<OrderResponse> listAllForAdmin(int page, int size);

       
                                                                         
                                      
       
    OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request);
}
