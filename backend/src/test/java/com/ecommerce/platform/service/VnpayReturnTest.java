package com.ecommerce.platform.service;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.common.PaymentTransactionStatus;
import com.ecommerce.platform.dto.response.VnpayReturnResponse;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.OrderItem;
import com.ecommerce.platform.entity.PaymentTransaction;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.repository.PaymentTransactionRepository;
import com.ecommerce.platform.service.impl.VnpayServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VnpayReturnTest {

    private PaymentTransactionRepository transactions;
    private OutboxNotificationService notifications;
    private ProductService products;
    private VnpayServiceImpl service;

    @BeforeEach
    void setUp() {
        transactions = mock(PaymentTransactionRepository.class);
        notifications = mock(OutboxNotificationService.class);
        products = mock(ProductService.class);
        service = new VnpayServiceImpl(transactions, notifications, products, new ObjectMapper());
        VnpayTestFixtures.configure(service);
    }

    @Test
    void verifiedCustomerCancellationCancelsOrderAndRestoresStockOnlyOnce() throws Exception {
        Order order = new Order();
        order.setId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        Product product = new Product();
        product.setId(7L);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        order.getItems().add(item);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setProvider(PaymentMethod.VNPAY);
        transaction.setProviderTxnRef("42-ref");
        transaction.setAmount(new BigDecimal("100000.00"));
        transaction.setStatus(PaymentTransactionStatus.INITIATED);
        when(transactions.findByProviderTxnRefForUpdate("42-ref")).thenReturn(Optional.of(transaction));

        Map<String, String> cancellation = VnpayTestFixtures.signedReturn("24", "02");
        VnpayReturnResponse first = service.handleReturn(cancellation);
        VnpayReturnResponse duplicate = service.handleReturn(cancellation);

        assertThat(first.isPaymentCancelled()).isTrue();
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(duplicate.isPaymentCancelled()).isFalse();
        verify(products, times(1)).restoreStockForCancelledOrder(7L, 2);
        verify(notifications).orderStatusChanged(42L, OrderStatus.CANCELLED);
    }

    @Test
    void invalidCancellationSignatureDoesNotLookUpOrChangePayment() throws Exception {
        Map<String, String> cancellation = VnpayTestFixtures.signedReturn("24", "02");
        cancellation.put("vnp_SecureHash", "invalid");

        VnpayReturnResponse response = service.handleReturn(cancellation);

        assertThat(response.isValidSignature()).isFalse();
        assertThat(response.isPaymentCancelled()).isFalse();
        verify(transactions, never()).findByProviderTxnRefForUpdate(any());
    }

    @Test
    void successfulBrowserReturnWaitsForIpnBeforeChangingOrder() throws Exception {
        Order order = new Order();
        order.setId(42L);
        order.setStatus(OrderStatus.PENDING);
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setProvider(PaymentMethod.VNPAY);
        transaction.setProviderTxnRef("42-ref");
        transaction.setAmount(new BigDecimal("100000.00"));
        transaction.setStatus(PaymentTransactionStatus.INITIATED);
        when(transactions.findByProviderTxnRefForUpdate("42-ref")).thenReturn(Optional.of(transaction));

        VnpayReturnResponse response = service.handleReturn(VnpayTestFixtures.signedReturn("00", "00"));

        assertThat(response.isValidSignature()).isTrue();
        assertThat(response.isPaymentSuccessful()).isTrue();
        assertThat(response.getMessage()).contains("Awaiting IPN");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.INITIATED);
        verify(notifications, never()).paymentSucceeded(any());
    }
}
