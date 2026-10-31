package com.ecommerce.platform.mail;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.OrderRepository;
import com.ecommerce.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

   
                                                                                       
                                                                                        
                                                 
   
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RetryingMailSender retryingMailSender;

    @Override
    public void sendWelcomeEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        retryingMailSender.send(user.getEmail(), "Welcome to E-Commerce Platform", "welcome", Map.of("user", user));
    }

    @Override
    public void sendOrderConfirmation(Long orderId) {
        Order order = findOrder(orderId);
        retryingMailSender.send(order.getUser().getEmail(), "Order #" + order.getId() + " confirmed", "order-confirmation", Map.of("order", order));
    }

    @Override
    public void sendPaymentConfirmation(Long orderId) {
        Order order = findOrder(orderId);
        retryingMailSender.send(order.getUser().getEmail(), "Payment received for order #" + order.getId(), "payment-confirmation", Map.of("order", order));
    }

    @Override
    public void sendOrderStatusChanged(Long orderId, OrderStatus status) {
        Order order = findOrder(orderId);
        retryingMailSender.send(order.getUser().getEmail(), "Order #" + order.getId() + " is " + status, "order-status", Map.of("order", order, "status", status));
    }
    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        retryingMailSender.send(email, "Reset your password", "password-reset", Map.of("resetUrl", resetUrl));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }
}
