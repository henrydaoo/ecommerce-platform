package com.ecommerce.platform.controller;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.request.CreateProductRequest;
import com.ecommerce.platform.dto.request.UpdateProductRequest;
import com.ecommerce.platform.dto.response.ProductResponse;
import com.ecommerce.platform.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.list(category, search, page, size));
    }

    @GetMapping("/admin")
    public ResponseEntity<PageResponse<ProductResponse>> listAllForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.listAllForAdmin(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

       
                                                                     
                                          
       
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

                                          
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        productService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
