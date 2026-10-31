package com.ecommerce.platform.dto.response;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.common.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private Instant createdAt;

       
                                                                           
                                                                                    
       
    private String paymentUrl;
}
