package com.ecommerce.platform.controller;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.common.Role;
import com.ecommerce.platform.dto.request.CheckoutRequest;
import com.ecommerce.platform.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.platform.dto.response.OrderResponse;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.security.UserPrincipal;
import com.ecommerce.platform.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    private OrderService orderService;
    private OrderController controller;
    private UserPrincipal customer;
    private UserPrincipal admin;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        controller = new OrderController(orderService);
        customer = principal(7L, Role.CUSTOMER);
        admin = principal(1L, Role.ADMIN);
    }

    @Test
    void checkoutUsesAuthenticatedCustomerAndIdempotencyKey() {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.VNPAY);
        OrderResponse order = order();
        when(orderService.checkout(7L, "checkout-123", request)).thenReturn(order);

        var response = controller.checkout(customer, "checkout-123", request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(order);
        verify(orderService).checkout(7L, "checkout-123", request);
    }

    @Test
    void listsCustomerOrdersAndResolvesOrderWithRoleFlag() {
        PageResponse<OrderResponse> page = new PageResponse<>(List.of(order()), 0, 20, 1, 1);
        when(orderService.listForUser(7L, 2, 5)).thenReturn(page);
        when(orderService.getById(7L, false, 9L)).thenReturn(order());
        when(orderService.getById(1L, true, 9L)).thenReturn(order());

        assertThat(controller.myOrders(customer, 2, 5).getBody()).isSameAs(page);
        controller.getById(customer, 9L);
        controller.getById(admin, 9L);

        verify(orderService).listForUser(7L, 2, 5);
        verify(orderService).getById(7L, false, 9L);
        verify(orderService).getById(1L, true, 9L);
    }

    @Test
    void forwardsAdminOrderListingAndStatusMutation() {
        PageResponse<OrderResponse> page = new PageResponse<>(List.of(order()), 0, 20, 1, 1);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.SHIPPED);
        when(orderService.listAllForAdmin(1, 10)).thenReturn(page);
        when(orderService.updateStatus(9L, request)).thenReturn(order());

        assertThat(controller.allOrders(1, 10).getBody()).isSameAs(page);
        assertThat(controller.updateStatus(9L, request).getBody()).isNotNull();
        verify(orderService).listAllForAdmin(1, 10);
        verify(orderService).updateStatus(9L, request);
    }

    private UserPrincipal principal(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setRole(role);
        return new UserPrincipal(user);
    }

    private OrderResponse order() {
        return new OrderResponse(9L, OrderStatus.PENDING, PaymentMethod.VNPAY,
                new BigDecimal("150.00"), List.of(), null, "https://payment.example");
    }
}
