package com.ecommerce.platform.messaging.outbox;

import com.ecommerce.platform.service.OutboxNotificationService;
import com.ecommerce.platform.config.TestMailConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfiguration.class)
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.messaging.outbox-poll-interval-ms=600000",
        "app.messaging.outbox-metrics-refresh-ms=600000"
})
class OutboxRelayIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final RabbitMQContainer rabbit =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @DynamicPropertySource
    static void configureServices(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OutboxRelay outboxRelay;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OutboxNotificationService outboxNotificationService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clearOutboxAndQueue() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        while (rabbitTemplate.receive("ecommerce.email.notifications") != null) {
                                                                               
        }
    }

    @Test
    void brokerConfirmPublishesMessageAndMarksOutboxEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO outbox_events
                    (event_id, event_type, schema_version, aggregate_type, aggregate_id, payload)
                VALUES (?, 'ORDER_CREATED', 1, 'ORDER', 42, CAST(? AS jsonb))
                """, eventId, "{\"orderId\":42}");

        outboxRelay.publishPendingEvents();

        Message message = rabbitTemplate.receive("ecommerce.email.notifications");
        assertNotNull(message, "Confirmed event should be present on the durable email queue");
        JsonNode envelope = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        assertEquals(eventId.toString(), envelope.get("eventId").asText());
        assertEquals("ORDER_CREATED", envelope.get("eventType").asText());
        assertEquals("PUBLISHED", jdbcTemplate.queryForObject(
                "SELECT status FROM outbox_events WHERE event_id = ?", String.class, eventId));
    }

    @Test
    void rollingBackBusinessTransactionAlsoRollsBackOutboxIntent() {
        transactionTemplate.executeWithoutResult(status -> {
            outboxNotificationService.orderCreated(84L);
            status.setRollbackOnly();
        });

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events", Integer.class));
    }
}
