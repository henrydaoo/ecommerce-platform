package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.request.CreateCategoryRequest;
import com.ecommerce.platform.dto.response.CategoryResponse;
import com.ecommerce.platform.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryControllerTest {

    private CategoryService categoryService;
    private CategoryController controller;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);
        controller = new CategoryController(categoryService);
    }

    @Test
    void listsCategories() {
        List<CategoryResponse> categories = List.of(new CategoryResponse(3L, "Audio", "audio"));
        when(categoryService.listAll()).thenReturn(categories);

        assertThat(controller.listAll().getBody()).isSameAs(categories);
        verify(categoryService).listAll();
    }

    @Test
    void createsAndUpdatesCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Audio");
        request.setSlug("audio");
        CategoryResponse category = new CategoryResponse(3L, "Audio", "audio");
        when(categoryService.create(request)).thenReturn(category);
        when(categoryService.update(3L, request)).thenReturn(category);

        var created = controller.create(request);
        var updated = controller.update(3L, request);

        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody()).isSameAs(category);
        assertThat(updated.getBody()).isSameAs(category);
        verify(categoryService).create(request);
        verify(categoryService).update(3L, request);
    }

    @Test
    void deletesCategoryAndReturnsNoContent() {
        var response = controller.delete(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(categoryService).delete(3L);
    }
}
