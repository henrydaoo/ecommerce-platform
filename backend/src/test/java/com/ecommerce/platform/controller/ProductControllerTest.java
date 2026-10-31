package com.ecommerce.platform.controller;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.request.CreateProductRequest;
import com.ecommerce.platform.dto.request.UpdateProductRequest;
import com.ecommerce.platform.dto.response.ProductResponse;
import com.ecommerce.platform.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    private ProductService productService;
    private ProductController controller;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        controller = new ProductController(productService);
    }

    @Test
    void forwardsCatalogFiltersAndPagination() {
        PageResponse<ProductResponse> page = page();
        when(productService.list(4L, "headphone", 2, 10)).thenReturn(page);

        var response = controller.list(4L, "headphone", 2, 10);

        assertThat(response.getBody()).isSameAs(page);
        verify(productService).list(4L, "headphone", 2, 10);
    }

    @Test
    void forwardsAdminListingAndProductLookup() {
        PageResponse<ProductResponse> page = page();
        ProductResponse product = product();
        when(productService.listAllForAdmin(1, 5)).thenReturn(page);
        when(productService.getById(8L)).thenReturn(product);

        assertThat(controller.listAllForAdmin(1, 5).getBody()).isSameAs(page);
        assertThat(controller.getById(8L).getBody()).isSameAs(product);
        verify(productService).listAllForAdmin(1, 5);
        verify(productService).getById(8L);
    }

    @Test
    void createsAndUpdatesProducts() {
        CreateProductRequest createRequest = new CreateProductRequest();
        UpdateProductRequest updateRequest = new UpdateProductRequest();
        ProductResponse product = product();
        when(productService.create(createRequest)).thenReturn(product);
        when(productService.update(8L, updateRequest)).thenReturn(product);

        var created = controller.create(createRequest);
        var updated = controller.update(8L, updateRequest);

        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody()).isSameAs(product);
        assertThat(updated.getBody()).isSameAs(product);
        verify(productService).create(createRequest);
        verify(productService).update(8L, updateRequest);
    }

    @Test
    void deactivatesAndReactivatesProducts() {
        assertThat(controller.delete(8L).getStatusCode().value()).isEqualTo(204);
        assertThat(controller.activate(8L).getStatusCode().value()).isEqualTo(204);
        verify(productService).delete(8L);
        verify(productService).activate(8L);
    }

    private PageResponse<ProductResponse> page() {
        return new PageResponse<>(List.of(product()), 1, 5, 1, 1);
    }

    private ProductResponse product() {
        return new ProductResponse(8L, "Headphones", "Description",
                new BigDecimal("45.00"), null, 3, null, true, 2);
    }
}
