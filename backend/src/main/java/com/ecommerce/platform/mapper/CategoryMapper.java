package com.ecommerce.platform.mapper;

import com.ecommerce.platform.dto.response.CategoryResponse;
import com.ecommerce.platform.entity.Category;
import org.mapstruct.Mapper;

                                                                                 
@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}
