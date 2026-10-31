package com.ecommerce.platform.controller;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.request.CheckoutRequest;
import com.ecommerce.platform.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.platform.dto.response.OrderResponse;
import com.ecommerce.platform.security.UserPrincipal;
import com.ecommerce.platform.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

                                                                                            
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.checkout(principal.getId(), idempotencyKey, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> myOrders(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.listForUser(principal.getId(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(principal.getId(), isAdmin(principal), id));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<PageResponse<OrderResponse>> allOrders(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.listAllForAdmin(page, size));
    }

       
                                                                              
                    
       
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
