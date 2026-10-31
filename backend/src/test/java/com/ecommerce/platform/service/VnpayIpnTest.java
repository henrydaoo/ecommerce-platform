package com.ecommerce.platform.service;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.common.PaymentTransactionStatus;
import com.ecommerce.platform.dto.response.VnpayIpnResponse;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.PaymentTransaction;
import com.ecommerce.platform.repository.PaymentTransactionRepository;
import com.ecommerce.platform.service.impl.VnpayServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VnpayIpnTest {

    private PaymentTransactionRepository transactions;
    private OutboxNotificationService notifications;
    private VnpayServiceImpl service;

    @BeforeEach
    void setUp() {
        transactions = mock(PaymentTransactionRepository.class);
        notifications = mock(OutboxNotificationService.class);
        service = new VnpayServiceImpl(
                transactions, notifications, mock(ProductService.class), new ObjectMapper());
        VnpayTestFixtures.configure(service);
    }

    @Test
    void verifiedIpnMarksPaymentOnceAndDuplicateIsNoOp() throws Exception {
        Order order = new Order();
        order.setId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100000.00"));
        PaymentTransaction transaction = transaction(order, PaymentTransactionStatus.INITIATED);
        when(transactions.findByProviderTxnRefForUpdate("42-ref")).thenReturn(Optional.of(transaction));

        Map<String, String> ipn = VnpayTestFixtures.signedIpn();
        VnpayIpnResponse first = service.handleIpn(ipn);
        VnpayIpnResponse duplicate = service.handleIpn(ipn);

        assertThat(first.getRspCode()).isEqualTo("00");
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(duplicate.getRspCode()).isEqualTo("02");
        verify(notifications).paymentSucceeded(42L);
    }

    @Test
    void invalidSignatureDoesNotLookUpOrPublishPayment() throws Exception {
        Map<String, String> ipn = VnpayTestFixtures.signedIpn();
        ipn.put("vnp_SecureHash", "invalid");

        VnpayIpnResponse response = service.handleIpn(ipn);

        assertThat(response.getRspCode()).isEqualTo("97");
        verify(transactions, never()).findByProviderTxnRefForUpdate(any());
        verify(notifications, never()).paymentSucceeded(any());
    }

    @Test
    void amountMismatchDoesNotMarkPaymentOrPublish() throws Exception {
        PaymentTransaction transaction = transaction(null, PaymentTransactionStatus.INITIATED);
        transaction.setAmount(new BigDecimal("100000.00"));
        when(transactions.findByProviderTxnRefForUpdate("42-ref")).thenReturn(Optional.of(transaction));
        Map<String, String> ipn = VnpayTestFixtures.signedIpn();
        ipn.put("vnp_Amount", "100");
        ipn.put("vnp_SecureHash", VnpayTestFixtures.signature(ipn));

        VnpayIpnResponse response = service.handleIpn(ipn);

        assertThat(response.getRspCode()).isEqualTo("04");
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.INITIATED);
        verify(notifications, never()).paymentSucceeded(any());
    }

    @Test
    void signedVnpayCallbackCannotUpdateAnotherPaymentProvider() throws Exception {
        PaymentTransaction transaction = transaction(null, PaymentTransactionStatus.INITIATED);
        transaction.setProvider(PaymentMethod.COD);
        when(transactions.findByProviderTxnRefForUpdate("42-ref")).thenReturn(Optional.of(transaction));

        VnpayIpnResponse response = service.handleIpn(VnpayTestFixtures.signedIpn());

        assertThat(response.getRspCode()).isEqualTo("01");
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.INITIATED);
        verify(notifications, never()).paymentSucceeded(any());
    }

    @Test
    void successfulIpnDoesNotReviveAnOrderThatIsNoLongerPending() throws Exception {
        Order order = new Order();
        order.setId(42L);
        order.setStatus(OrderStatus.CANCELLED);
        PaymentTransaction transaction = transaction(order, PaymentTransactionStatus.INITIATED);
        when(transactions.findByProviderTxnRefForUpdate("42-ref")).thenReturn(Optional.of(transaction));

        VnpayIpnResponse response = service.handleIpn(VnpayTestFixtures.signedIpn());

        assertThat(response.getRspCode()).isEqualTo("02");
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.INITIATED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(notifications, never()).paymentSucceeded(any());
    }

    @Test
    void failedPaymentCannotTransitionToSuccessOnLaterCallback() throws Exception {
        PaymentTransaction transaction = transaction(null, PaymentTransactionStatus.FAILED);
        when(transactions.findByProviderTxnRefForUpdate("42-ref")).thenReturn(Optional.of(transaction));

        VnpayIpnResponse response = service.handleIpn(VnpayTestFixtures.signedIpn());

        assertThat(response.getRspCode()).isEqualTo("02");
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        verify(notifications, never()).paymentSucceeded(any());
    }

    private PaymentTransaction transaction(Order order, PaymentTransactionStatus status) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setProvider(PaymentMethod.VNPAY);
        transaction.setAmount(new BigDecimal("100000.00"));
        transaction.setStatus(status);
        return transaction;
    }
}
