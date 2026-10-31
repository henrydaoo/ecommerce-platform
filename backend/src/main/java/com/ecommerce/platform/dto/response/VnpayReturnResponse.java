package com.ecommerce.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

                                                             
@Getter
@AllArgsConstructor
public class VnpayReturnResponse {
    private boolean validSignature;
    private boolean paymentSuccessful;
    private boolean paymentCancelled;
    private String transactionReference;
    private String responseCode;
    private String message;
}
