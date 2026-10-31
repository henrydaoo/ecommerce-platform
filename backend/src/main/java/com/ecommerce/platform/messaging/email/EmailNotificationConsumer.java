package com.ecommerce.platform.messaging.email;

import com.ecommerce.platform.common.OrderStatus;
import com.ecommerce.platform.service.EmailDeadLetterService;
import com.ecommerce.platform.mail.MailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final MailService mailService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final EmailDeadLetterService deadLetterService;

    @RabbitListener(queues = "${app.messaging.email-queue}", concurrency = "${app.messaging.email-consumer-concurrency:1}")
    @Retryable(retryFor = Exception.class, maxAttemptsExpression = "${app.messaging.email-consumer-attempts}", backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void consume(EmailNotificationMessage message) {
        try {
            if (message.schemaVersion() != 1) {
                throw new IllegalArgumentException(
                        "Unsupported email event schema version: " + message.schemaVersion());
            }
            if (isDelivered(message.eventId())) {
                log.info("Ignoring previously delivered email event eventId={} eventType={}",
                        message.eventId(), message.eventType());
                deadLetterService.markResolved(message.eventId());
                return;
            }

            switch (message.eventType()) {
                case "USER_REGISTERED" -> mailService.sendWelcomeEmail(longValue(message, "userId"));
                case "ORDER_CREATED" -> mailService.sendOrderConfirmation(longValue(message, "orderId"));
                case "PAYMENT_SUCCEEDED" -> mailService.sendPaymentConfirmation(longValue(message, "orderId"));
                case "ORDER_STATUS_CHANGED" -> mailService.sendOrderStatusChanged(
                        longValue(message, "orderId"),
                        OrderStatus.valueOf(stringValue(message, "status")));
                default -> throw new IllegalArgumentException(
                        "Unsupported email event type: " + message.eventType());
            }

            markDelivered(message.eventId(), message.eventType());
            deadLetterService.markResolved(message.eventId());
            meterRegistry.counter("app.email.notification.delivered").increment();
        } catch (RuntimeException exception) {
            meterRegistry.counter("app.email.notification.failure").increment();
            log.warn("Email notification attempt failed eventId={} eventType={} cause={}",
                    message.eventId(), message.eventType(), exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private boolean isDelivered(UUID eventId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM email_notification_deliveries WHERE event_id = ?)",
                Boolean.class, eventId));
    }

    private void markDelivered(UUID eventId, String eventType) {
        jdbcTemplate.update("""
                INSERT INTO email_notification_deliveries (event_id, event_type, delivered_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType);
    }

    private Long longValue(EmailNotificationMessage message, String key) {
        Object value = message.payload().get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + key + " in event " + message.eventId());
        }
        return objectMapper.convertValue(value, Long.class);
    }

    private String stringValue(EmailNotificationMessage message, String key) {
        Object value = message.payload().get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + key + " in event " + message.eventId());
        }
        return value.toString();
    }
}
