package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.response.VnpayIpnResponse;
import com.ecommerce.platform.dto.response.VnpayReturnResponse;
import com.ecommerce.platform.service.VnpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment/vnpay")
@RequiredArgsConstructor
public class PaymentController {
    private final VnpayService vnpayService;

       
                                                                                  
                     
       
    @GetMapping("/return")
    public ResponseEntity<VnpayReturnResponse> paymentReturn(@RequestParam Map<String, String> parameters) {
        return ResponseEntity.ok(vnpayService.handleReturn(parameters));
    }

    @GetMapping("/ipn")
    public ResponseEntity<VnpayIpnResponse> ipn(@RequestParam Map<String, String> parameters) {
        return ResponseEntity.ok(vnpayService.handleIpn(parameters));
    }
}
