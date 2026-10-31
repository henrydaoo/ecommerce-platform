package com.ecommerce.platform.service;

import com.ecommerce.platform.common.OrderStatus;

public interface OutboxNotificationService {
    void userRegistered(Long userId);

    void orderCreated(Long orderId);

    void paymentSucceeded(Long orderId);

    void orderStatusChanged(Long orderId, OrderStatus status);
}
