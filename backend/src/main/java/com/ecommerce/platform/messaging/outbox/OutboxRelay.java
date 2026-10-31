package com.ecommerce.platform.messaging.outbox;

import com.ecommerce.platform.messaging.email.EmailNotificationMessage;
import com.ecommerce.platform.repository.OutboxRecord;
import com.ecommerce.platform.repository.OutboxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${app.messaging.email-exchange}")
    private String exchange;
    @Value("${app.messaging.email-routing-key}")
    private String routingKey;
    @Value("${app.messaging.outbox-batch-size}")
    private int batchSize;
    @Value("${app.messaging.outbox-lease-seconds}")
    private int leaseSeconds;
    @Value("${app.messaging.outbox-max-attempts}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.messaging.outbox-poll-interval-ms}")
    public void publishPendingEvents() {
        List<OutboxRecord> batch = transactionTemplate.execute(
                ignored -> outboxRepository.claimBatch(batchSize, leaseSeconds));
        if (batch == null || batch.isEmpty()) {
            return;
        }

        for (OutboxRecord record : batch) {
            publish(record);
        }
    }

    private void publish(OutboxRecord record) {
        try {
            Map<String, Object> payload = objectMapper.readValue(record.payload(), PAYLOAD_TYPE);
            EmailNotificationMessage message = new EmailNotificationMessage(
                    record.eventId(), record.eventType(), record.schemaVersion(),
                    record.aggregateId(), record.occurredAt(), payload);
            CorrelationData correlation = new CorrelationData(record.eventId().toString());
            rabbitTemplate.convertAndSend(exchange, routingKey, message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ negatively acknowledged the message");
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ returned an unroutable notification");
            }

            transactionTemplate.executeWithoutResult(
                    ignored -> outboxRepository.markPublished(record.eventId()));
            log.info("Outbox event published eventId={} eventType={}",
                    record.eventId(), record.eventType());
            meterRegistry.counter("app.outbox.publish.success").increment();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            recordFailure(record, exception);
        } catch (Exception exception) {
            recordFailure(record, exception);
        }
    }

    private void recordFailure(OutboxRecord record, Exception exception) {
        transactionTemplate.executeWithoutResult(ignored -> outboxRepository.markFailed(
                record.eventId(), record.attemptCount(), maxAttempts,
                exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        meterRegistry.counter("app.outbox.publish.failure").increment();
        log.error("Unable to publish outbox event eventId={} eventType={} attempt={}",
                record.eventId(), record.eventType(), record.attemptCount(), exception);
    }
}
