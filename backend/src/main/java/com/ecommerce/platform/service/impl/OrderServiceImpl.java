package com.ecommerce.platform.service.impl;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.request.CheckoutRequest;
import com.ecommerce.platform.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.platform.dto.response.OrderItemResponse;
import com.ecommerce.platform.dto.response.OrderResponse;
import com.ecommerce.platform.entity.Cart;
import com.ecommerce.platform.entity.CartItem;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.OrderItem;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.exception.ApiException;
import com.ecommerce.platform.exception.EmptyCartException;
import com.ecommerce.platform.exception.ErrorCode;
import com.ecommerce.platform.exception.InvalidStateTransitionException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.CartRepository;
import com.ecommerce.platform.repository.OrderRepository;
import com.ecommerce.platform.service.OrderService;
import com.ecommerce.platform.service.OutboxNotificationService;
import com.ecommerce.platform.service.ProductService;
import com.ecommerce.platform.service.VnpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductService productService;
    private final VnpayService vnpayService;
    private final OutboxNotificationService outboxNotificationService;

    @Override
    @Transactional
    public OrderResponse checkout(Long userId, String idempotencyKey, CheckoutRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }

                                                                              
                                                                               
        var existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get(), paymentUrlFor(existing.get()));
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", userId));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

                                                                               
                                                                           
                                     
        List<CartItem> sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(ci -> ci.getProduct().getId()))
                .toList();

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setIdempotencyKey(idempotencyKey);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : sortedItems) {
                                                                             
                                                                          
                                                                       
                                                 
            Product locked = productService.decrementStockForCheckout(cartItem.getProduct().getId(),
                    cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(locked);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(locked.getPrice());
            orderItems.add(orderItem);

            total = total.add(locked.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(total);
        order.setItems(orderItems);
        orderRepository.saveAndFlush(order);

        String paymentUrl = paymentUrlFor(order);

                                                                              
                                     
        cart.getItems().clear();

        outboxNotificationService.orderCreated(order.getId());

        return toResponse(order, paymentUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findByUserId(userId, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

       
                                                                                   
                                          
       
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long userId, boolean isAdmin, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listAllForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findAll(pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getStatus().canTransitionTo(request.getStatus())) {
            throw new InvalidStateTransitionException(order.getStatus().name(), request.getStatus().name());
        }
        if (order.getPaymentMethod() == PaymentMethod.VNPAY
                && (request.getStatus() == OrderStatus.PAID || request.getStatus() == OrderStatus.CANCELLED)) {
            throw new ApiException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "VNPay payment and cancellation status must be confirmed by a verified payment flow");
        }
        if (order.getStatus() == OrderStatus.PAID && request.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "Paid orders cannot be cancelled until refunds are supported");
        }
        if (order.getStatus() == OrderStatus.PENDING && request.getStatus() == OrderStatus.CANCELLED) {
            order.getItems().stream()
                    .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                    .forEach(item -> productService.restoreStockForCancelledOrder(
                            item.getProduct().getId(), item.getQuantity()));
        }
        order.setStatus(request.getStatus());
        outboxNotificationService.orderStatusChanged(order.getId(), order.getStatus());

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }

    private OrderResponse toResponse(Order order, String paymentUrl) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(oi -> new OrderItemResponse(
                        oi.getProduct().getId(),
                        oi.getProduct().getName(),
                        oi.getQuantity(),
                        oi.getUnitPrice(),
                        oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity()))))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt(),
                paymentUrl);
    }

    private String paymentUrlFor(Order order) {
        return order.getPaymentMethod() == PaymentMethod.VNPAY && order.getStatus() == OrderStatus.PENDING
                ? vnpayService.createPaymentUrl(order, null)
                : null;
    }
}
