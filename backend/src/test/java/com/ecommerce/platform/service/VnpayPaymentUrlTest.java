package com.ecommerce.platform.service;

import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.common.PaymentTransactionStatus;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.PaymentTransaction;
import com.ecommerce.platform.repository.PaymentTransactionRepository;
import com.ecommerce.platform.service.impl.VnpayServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VnpayPaymentUrlTest {

    private PaymentTransactionRepository transactions;
    private VnpayServiceImpl service;

    @BeforeEach
    void setUp() {
        transactions = mock(PaymentTransactionRepository.class);
        service = new VnpayServiceImpl(
                transactions, mock(OutboxNotificationService.class), mock(ProductService.class), new ObjectMapper());
        VnpayTestFixtures.configure(service);
        ReflectionTestUtils.setField(service, "payUrl", "https://sandbox.example/pay");
        ReflectionTestUtils.setField(service, "returnUrl", "https://shop.example/return");
    }

    @Test
    void paymentUrlUsesConfiguredMerchantAmountAndVietnamLocalTime() throws Exception {
        Order order = order();
        when(transactions.findFirstByOrderIdAndProviderOrderByCreatedAtDesc(42L, PaymentMethod.VNPAY))
                .thenReturn(Optional.empty());
        when(transactions.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        String paymentUrl = service.createPaymentUrl(order, "127.0.0.1");

        Instant after = Instant.now();
        Map<String, String> parameters = VnpayTestFixtures.queryParameters(paymentUrl);
        assertThat(parameters.get("vnp_TmnCode")).isEqualTo("test-merchant");
        assertThat(parameters.get("vnp_Amount")).isEqualTo("10000000");
        assertThat(parameters.get("vnp_ReturnUrl")).isEqualTo("https://shop.example/return");
        assertThat(parameters.get("vnp_OrderInfo")).isEqualTo("Order 42");
        assertThat(paymentUrl).contains("vnp_OrderInfo=Order+42");
        assertThat(parameters.get("vnp_SecureHash")).isEqualTo(VnpayTestFixtures.signature(parameters));

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime createDate = LocalDateTime.parse(parameters.get("vnp_CreateDate"), format);
        LocalDateTime earliest = LocalDateTime.ofInstant(before, ZoneId.of("Asia/Ho_Chi_Minh")).minusSeconds(1);
        LocalDateTime latest = LocalDateTime.ofInstant(after, ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(1);
        assertThat(createDate).isBetween(earliest, latest);
    }

    @Test
    void failedPaymentAttemptGetsANewTransactionReferenceOnRetry() {
        Order order = order();
        PaymentTransaction failedAttempt = new PaymentTransaction();
        failedAttempt.setStatus(PaymentTransactionStatus.FAILED);
        failedAttempt.setProviderTxnRef("42-old");
        when(transactions.findFirstByOrderIdAndProviderOrderByCreatedAtDesc(42L, PaymentMethod.VNPAY))
                .thenReturn(Optional.of(failedAttempt));
        when(transactions.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String paymentUrl = service.createPaymentUrl(order, "127.0.0.1");

        assertThat(VnpayTestFixtures.queryParameters(paymentUrl).get("vnp_TxnRef")).isNotEqualTo("42-old");
        verify(transactions).save(any(PaymentTransaction.class));
    }

    private Order order() {
        Order order = new Order();
        order.setId(42L);
        order.setTotalAmount(new BigDecimal("100000.00"));
        return order;
    }
}
