package com.ecommerce.platform.service;

import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.exception.CategoryInUseException;
import com.ecommerce.platform.mapper.CategoryMapper;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import com.ecommerce.platform.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceDeletionTest {

    @Test
    void categoryCannotBeDeletedWhileAnyProductReferencesIt() {
        CategoryRepository categories = mock(CategoryRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        Category category = new Category();
        category.setId(5L);
        when(categories.findById(5L)).thenReturn(Optional.of(category));
        when(products.existsByCategoryId(5L)).thenReturn(true);
        CategoryServiceImpl service = new CategoryServiceImpl(
                categories, products, mock(CategoryMapper.class));

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(CategoryInUseException.class);

        verify(categories, never()).delete(category);
    }
}
