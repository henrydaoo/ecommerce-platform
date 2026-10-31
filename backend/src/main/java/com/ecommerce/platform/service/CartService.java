package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.request.AddCartItemRequest;
import com.ecommerce.platform.dto.request.UpdateCartItemRequest;
import com.ecommerce.platform.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

                                                                                                                  
    CartResponse addItem(Long userId, AddCartItemRequest request);

    CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, Long itemId);
}
