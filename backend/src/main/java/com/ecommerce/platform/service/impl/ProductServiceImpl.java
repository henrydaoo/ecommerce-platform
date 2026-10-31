package com.ecommerce.platform.service.impl;

import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.platform.dto.request.CreateProductRequest;
import com.ecommerce.platform.dto.request.UpdateProductRequest;
import com.ecommerce.platform.dto.response.ProductResponse;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.exception.InsufficientStockException;
import com.ecommerce.platform.exception.OptimisticLockConflictException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.mapper.ProductMapper;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import com.ecommerce.platform.service.ProductService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

                                                                                
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(Long categoryId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasCategory = categoryId != null;
        boolean hasSearch = StringUtils.hasText(search);

        Page<Product> result;
        if (hasCategory && hasSearch) {
            result = productRepository.findByActiveTrueAndCategoryIdAndNameContainingIgnoreCase(
                    categoryId, search, pageable);
        } else if (hasCategory) {
            result = productRepository.findByActiveTrueAndCategoryId(categoryId, pageable);
        } else if (hasSearch) {
            result = productRepository.findByActiveTrueAndNameContainingIgnoreCase(search, pageable);
        } else {
            result = productRepository.findByActiveTrue(pageable);
        }

        return PageResponse.from(result.map(productMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listAllForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(productRepository.findAll(pageable).map(productMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product", id);
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setActive(true);

        productRepository.save(product);
        return productMapper.toResponse(product);
    }

       
                                                                              
                                                                           
                                            
       
    @Override
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (!product.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockConflictException("Product", id);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);

        try {
            productRepository.saveAndFlush(product);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
            throw new OptimisticLockConflictException("Product", id);
        }

        return productMapper.toResponse(product);
    }

       
                                                                                 
                             
       
    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(true);
    }

    @Override
    @Transactional
    public Product decrementStockForCheckout(Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (!product.isActive() || product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, product.getStockQuantity());
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        return product;
    }

    @Override
    @Transactional
    public void restoreStockForCancelledOrder(Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        product.setStockQuantity(Math.addExact(product.getStockQuantity(), quantity));
    }
}
