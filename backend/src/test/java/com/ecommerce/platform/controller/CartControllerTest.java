package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.request.AddCartItemRequest;
import com.ecommerce.platform.dto.request.UpdateCartItemRequest;
import com.ecommerce.platform.dto.response.CartResponse;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.common.Role;
import com.ecommerce.platform.security.UserPrincipal;
import com.ecommerce.platform.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartControllerTest {

    private CartService cartService;
    private CartController controller;
    private UserPrincipal customer;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        controller = new CartController(cartService);
        User user = new User();
        user.setId(7L);
        user.setEmail("customer@example.com");
        user.setRole(Role.CUSTOMER);
        customer = new UserPrincipal(user);
    }

    @Test
    void returnsCurrentCustomersCart() {
        CartResponse cart = cart();
        when(cartService.getCart(7L)).thenReturn(cart);

        var response = controller.getCart(customer);

        assertThat(response.getBody()).isSameAs(cart);
        verify(cartService).getCart(7L);
    }

    @Test
    void addsItemAndReturnsCreatedWithUpdatedCart() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(15L);
        request.setQuantity(2);
        CartResponse cart = cart();
        when(cartService.addItem(7L, request)).thenReturn(cart);

        var response = controller.addItem(customer, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(cart);
        verify(cartService).addItem(7L, request);
    }

    @Test
    void updatesAndRemovesOnlyTheCustomersCartItem() {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(3);
        CartResponse updatedCart = cart();
        when(cartService.updateItem(7L, 19L, request)).thenReturn(updatedCart);
        when(cartService.removeItem(7L, 19L)).thenReturn(updatedCart);

        assertThat(controller.updateItem(customer, 19L, request).getBody()).isSameAs(updatedCart);
        assertThat(controller.removeItem(customer, 19L).getBody()).isSameAs(updatedCart);
        verify(cartService).updateItem(7L, 19L, request);
        verify(cartService).removeItem(7L, 19L);
    }

    private CartResponse cart() {
        return new CartResponse(2L, List.of(), BigDecimal.ZERO);
    }
}
