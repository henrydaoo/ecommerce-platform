package com.ecommerce.platform.service;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.OrderItem;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.exception.ApiException;
import com.ecommerce.platform.exception.InvalidStateTransitionException;
import com.ecommerce.platform.repository.CartRepository;
import com.ecommerce.platform.repository.OrderRepository;
import com.ecommerce.platform.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceCancellationTest {

    @Test
    void rejectsOrderStatusTransitionsThatSkipLifecycleStates() {
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findByIdForUpdate(9L)).thenReturn(Optional.of(order(PaymentMethod.COD, OrderStatus.PENDING)));
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> service(orders, mock(ProductService.class)).updateStatus(9L, request))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void onlyVerifiedPaymentFlowCanMarkVnpayOrderPaid() {
        OrderRepository orders = mock(OrderRepository.class);
        Order order = order(PaymentMethod.VNPAY, OrderStatus.PENDING);
        when(orders.findByIdForUpdate(9L)).thenReturn(Optional.of(order));
        ProductService products = mock(ProductService.class);
        OrderServiceImpl service = service(orders, products);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.PAID);

        assertThatThrownBy(() -> service.updateStatus(9L, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void adminCanMarkPendingCodOrderPaidAndPublishesStatusChange() {
        OrderRepository orders = mock(OrderRepository.class);
        Order order = order(PaymentMethod.COD, OrderStatus.PENDING);
        when(orders.findByIdForUpdate(9L)).thenReturn(Optional.of(order));
        ProductService products = mock(ProductService.class);
        OutboxNotificationService notifications = mock(OutboxNotificationService.class);
        OrderServiceImpl service = new OrderServiceImpl(
                orders, mock(CartRepository.class), products, mock(VnpayService.class), notifications);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.PAID);

        service.updateStatus(9L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(notifications).orderStatusChanged(9L, OrderStatus.PAID);
    }

    @Test
    void paidOrderCannotBeCancelledUntilRefundsAreSupported() {
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findByIdForUpdate(9L)).thenReturn(Optional.of(order(PaymentMethod.COD, OrderStatus.PAID)));
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> service(orders, mock(ProductService.class)).updateStatus(9L, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cancellingUnpaidCodOrderRestoresItsReservedStock() {
        OrderRepository orders = mock(OrderRepository.class);
        ProductService products = mock(ProductService.class);
        Order order = order(PaymentMethod.COD, OrderStatus.PENDING);
        Product product = new Product();
        product.setId(17L);
        product.setName("Test item");
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("25.00"));
        order.getItems().add(item);
        when(orders.findByIdForUpdate(9L)).thenReturn(Optional.of(order));
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CANCELLED);

        service(orders, products).updateStatus(9L, request);

        verify(products).restoreStockForCancelledOrder(17L, 2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private OrderServiceImpl service(OrderRepository orders, ProductService products) {
        return new OrderServiceImpl(
                orders,
                mock(CartRepository.class),
                products,
                mock(VnpayService.class),
                mock(OutboxNotificationService.class));
    }

    private Order order(PaymentMethod method, OrderStatus status) {
        Order order = new Order();
        order.setId(9L);
        order.setPaymentMethod(method);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("50.00"));
        return order;
    }
}
