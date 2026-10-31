package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.request.UpdateProductRequest;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.exception.OptimisticLockConflictException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.mapper.ProductMapper;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import com.ecommerce.platform.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServiceAdminUpdateTest {

    private ProductRepository products;
    private CategoryRepository categories;
    private ProductMapper mapper;
    private ProductServiceImpl productService;
    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        categories = mock(CategoryRepository.class);
        mapper = mock(ProductMapper.class);
        productService = new ProductServiceImpl(products, categories, mapper);

        product = new Product();
        product.setId(8L);
        product.setVersion(3);
        category = new Category();
        category.setId(5L);

        when(products.findById(8L)).thenReturn(Optional.of(product));
        when(categories.findById(5L)).thenReturn(Optional.of(category));
    }

    @Test
    void appliesAdminEditsAndPersistsTheVersionedProduct() {
        productService.update(8L, request(3));

        assertThat(product.getName()).isEqualTo("Updated product");
        assertThat(product.getDescription()).isEqualTo("Updated description");
        assertThat(product.getPrice()).isEqualByComparingTo("125.50");
        assertThat(product.getImageUrl()).isEqualTo("https://example.test/image.png");
        assertThat(product.getStockQuantity()).isEqualTo(12);
        assertThat(product.getCategory()).isSameAs(category);
        verify(products).saveAndFlush(product);
        verify(mapper).toResponse(product);
    }

    @Test
    void rejectsStaleEditBeforeLoadingCategoryOrSaving() {
        assertThatThrownBy(() -> productService.update(8L, request(2)))
                .isInstanceOf(OptimisticLockConflictException.class);

        verifyNoInteractions(categories);
        verify(products, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void rejectsEditWhenCategoryDoesNotExist() {
        when(categories.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(8L, request(3)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(products, never()).saveAndFlush(any(Product.class));
    }

    private UpdateProductRequest request(int version) {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated product");
        request.setDescription("Updated description");
        request.setPrice(new BigDecimal("125.50"));
        request.setImageUrl("https://example.test/image.png");
        request.setStockQuantity(12);
        request.setCategoryId(5L);
        request.setVersion(version);
        return request;
    }
}
