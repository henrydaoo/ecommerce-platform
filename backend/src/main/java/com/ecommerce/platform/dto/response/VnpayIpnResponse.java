package com.ecommerce.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

                                                                                      
@Getter
@AllArgsConstructor
public class VnpayIpnResponse {
    private String RspCode;
    private String Message;
}
