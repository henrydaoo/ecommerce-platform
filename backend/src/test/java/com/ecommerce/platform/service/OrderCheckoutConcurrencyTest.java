package com.ecommerce.platform.service;

import com.ecommerce.platform.common.PaymentMethod;
import com.ecommerce.platform.common.Role;
import com.ecommerce.platform.config.TestMailConfiguration;
import com.ecommerce.platform.dto.request.CheckoutRequest;
import com.ecommerce.platform.entity.Cart;
import com.ecommerce.platform.entity.CartItem;
import com.ecommerce.platform.entity.Category;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.exception.InsufficientStockException;
import com.ecommerce.platform.repository.CartItemRepository;
import com.ecommerce.platform.repository.CartRepository;
import com.ecommerce.platform.repository.CategoryRepository;
import com.ecommerce.platform.repository.ProductRepository;
import com.ecommerce.platform.repository.UserRepository;
import com.ecommerce.platform.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
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
class OrderCheckoutConcurrencyTest {

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
    private OrderService orderService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long buyerAId;
    private Long buyerBId;
    private Long raceProductId;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Race Category");
        category.setSlug("race-category-" + System.nanoTime());
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Last Unit Product");
        product.setDescription("Used to test the checkout stock race condition");
        product.setPrice(new BigDecimal("19.99"));
        product.setStockQuantity(1);                                                           
        product.setCategory(category);
        product.setActive(true);
        productRepository.save(product);
        raceProductId = product.getId();

        buyerAId = createBuyerWithCartItem("buyer-a-" + System.nanoTime() + "@test.com", product);
        buyerBId = createBuyerWithCartItem("buyer-b-" + System.nanoTime() + "@test.com", product);
    }

    private Long createBuyerWithCartItem(String email, Product product) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("not-a-real-hash");
        user.setFullName("Test Buyer");
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);
        cartItemRepository.save(item);

        return user.getId();
    }

    @Test
    void twoCustomersCheckingOutTheLastUnit_onlyOneOrderSucceeds() throws InterruptedException {
        CheckoutRequest requestA = new CheckoutRequest();
        requestA.setPaymentMethod(PaymentMethod.COD);
        CheckoutRequest requestB = new CheckoutRequest();
        requestB.setPaymentMethod(PaymentMethod.COD);

        String idempotencyKeyA = UUID.randomUUID().toString();
        String idempotencyKeyB = UUID.randomUUID().toString();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        executor.submit(() -> runCheckout(buyerAId, idempotencyKeyA, requestA,
                readyLatch, startLatch, doneLatch, successCount, insufficientStockCount));
        executor.submit(() -> runCheckout(buyerBId, idempotencyKeyB, requestB,
                readyLatch, startLatch, doneLatch, successCount, insufficientStockCount));

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();                                                              
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Both checkout attempts should complete without deadlocking");
        assertEquals(1, successCount.get(), "Exactly one customer's checkout should succeed on stock=1");
        assertEquals(1, insufficientStockCount.get(), "The other customer's checkout should be rejected as out of stock");

        Product finalState = productRepository.findById(raceProductId).orElseThrow();
        assertEquals(0, finalState.getStockQuantity(), "Stock must never go negative and must land at exactly 0");
        Integer orderEmails = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type = 'ORDER_CREATED'",
                Integer.class);
        assertEquals(1, orderEmails, "Only the committed checkout should create an order email intent");
    }

    private void runCheckout(Long buyerId, String idempotencyKey, CheckoutRequest request,
                              CountDownLatch readyLatch, CountDownLatch startLatch, CountDownLatch doneLatch,
                              AtomicInteger successCount, AtomicInteger insufficientStockCount) {
        try {
            readyLatch.countDown();
            startLatch.await();
            orderService.checkout(buyerId, idempotencyKey, request);
            successCount.incrementAndGet();
        } catch (InsufficientStockException ex) {
            insufficientStockCount.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            doneLatch.countDown();
        }
    }
}
