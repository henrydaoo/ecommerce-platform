package com.ecommerce.platform.service;

import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.config.TestMailConfiguration;
import com.ecommerce.platform.exception.InsufficientStockException;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

   
                                                                            
                                                                          
                                                                      
                                                                           
                             
   
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfiguration.class)
class ProductServiceConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long raceProductId;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Race Category");
        category.setSlug("race-category-" + System.nanoTime());
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Last Unit Product");
        product.setDescription("Used to test the stock race condition");
        product.setPrice(new BigDecimal("9.99"));
        product.setStockQuantity(1);                                                           
        product.setCategory(category);
        product.setActive(true);
        productRepository.save(product);

        raceProductId = product.getId();
    }

    @Test
    void twoConcurrentCheckoutsOnStockOne_onlyOneSucceeds() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();                                            
                    productService.decrementStockForCheckout(raceProductId, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    insufficientStockCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();                                                              
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Both checkout attempts should complete without deadlocking");
        assertEquals(1, successCount.get(), "Exactly one checkout should succeed on stock=1");
        assertEquals(1, insufficientStockCount.get(), "The other checkout should be rejected as out of stock");

        Product finalState = productRepository.findById(raceProductId).orElseThrow();
        assertEquals(0, finalState.getStockQuantity(), "Stock must never go negative and must land at exactly 0");
    }

    @Test
    void checkoutRejectsWhenQuantityExceedsStock() {
        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> productService.decrementStockForCheckout(raceProductId, 2));
        assertNotNull(ex.getMessage());

        Product unchanged = productRepository.findById(raceProductId).orElseThrow();
        assertEquals(1, unchanged.getStockQuantity(), "Stock must be unchanged when the request is rejected");
    }
}
