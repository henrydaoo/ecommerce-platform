package com.ecommerce.platform.service;

import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.mapper.ProductMapper;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import com.ecommerce.platform.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceVisibilityTest {

    @Test
    void inactiveProductIsNotVisibleByPublicId() {
        ProductRepository products = mock(ProductRepository.class);
        Product product = new Product();
        product.setId(8L);
        product.setActive(false);
        when(products.findById(8L)).thenReturn(Optional.of(product));
        ProductServiceImpl service = new ProductServiceImpl(
                products, mock(CategoryRepository.class), mock(ProductMapper.class));

        assertThatThrownBy(() -> service.getById(8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void restoresStockUnderProductLock() {
        ProductRepository products = mock(ProductRepository.class);
        Product product = new Product();
        product.setId(8L);
        product.setStockQuantity(4);
        when(products.findByIdForUpdate(8L)).thenReturn(Optional.of(product));
        ProductServiceImpl service = new ProductServiceImpl(
                products, mock(CategoryRepository.class), mock(ProductMapper.class));

        service.restoreStockForCancelledOrder(8L, 3);

        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void adminCanReactivateAnInactiveProduct() {
        ProductRepository products = mock(ProductRepository.class);
        Product product = new Product();
        product.setId(8L);
        product.setActive(false);
        when(products.findById(8L)).thenReturn(Optional.of(product));
        ProductServiceImpl service = new ProductServiceImpl(
                products, mock(CategoryRepository.class), mock(ProductMapper.class));

        service.activate(8L);

        assertThat(product.isActive()).isTrue();
    }

    @Test
    void reactivationRequiresAnExistingProduct() {
        ProductRepository products = mock(ProductRepository.class);
        when(products.findById(8L)).thenReturn(Optional.empty());
        ProductServiceImpl service = new ProductServiceImpl(
                products, mock(CategoryRepository.class), mock(ProductMapper.class));

        assertThatThrownBy(() -> service.activate(8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
