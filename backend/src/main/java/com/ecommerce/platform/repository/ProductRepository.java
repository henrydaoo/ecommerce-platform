package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

       
                                                                             
                                                                             
                                                                         
                                                    
       
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByActiveTrueAndCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String search, Pageable pageable);

    Page<Product> findByActiveTrueAndCategoryIdAndNameContainingIgnoreCase(
            Long categoryId, String search, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}
