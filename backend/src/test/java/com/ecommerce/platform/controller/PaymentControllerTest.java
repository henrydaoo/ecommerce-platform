package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.response.VnpayIpnResponse;
import com.ecommerce.platform.dto.response.VnpayReturnResponse;
import com.ecommerce.platform.service.VnpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    private VnpayService vnpayService;
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        vnpayService = mock(VnpayService.class);
        controller = new PaymentController(vnpayService);
    }

    @Test
    void forwardsBrowserReturnParametersForVerification() {
        Map<String, String> parameters = Map.of("vnp_TxnRef", "9-ref", "vnp_ResponseCode", "00");
        VnpayReturnResponse result = new VnpayReturnResponse(true, true, false, "9-ref", "00", "Awaiting IPN");
        when(vnpayService.handleReturn(parameters)).thenReturn(result);

        var response = controller.paymentReturn(parameters);

        assertThat(response.getBody()).isSameAs(result);
        verify(vnpayService).handleReturn(parameters);
    }

    @Test
    void forwardsIpnParametersForAuthoritativePaymentProcessing() {
        Map<String, String> parameters = Map.of("vnp_TxnRef", "9-ref", "vnp_TransactionStatus", "00");
        VnpayIpnResponse result = new VnpayIpnResponse("00", "Confirm success");
        when(vnpayService.handleIpn(parameters)).thenReturn(result);

        var response = controller.ipn(parameters);

        assertThat(response.getBody()).isSameAs(result);
        verify(vnpayService).handleIpn(parameters);
    }
}
