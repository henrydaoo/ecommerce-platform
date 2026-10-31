package com.ecommerce.platform.messaging.dlq;

import com.ecommerce.platform.messaging.email.EmailNotificationMessage;
import com.ecommerce.platform.repository.EmailDeadLetterRepository;
import com.rabbitmq.client.Channel;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailDeadLetterConsumerTest {

    @Test
    void acknowledgesMessageAfterItIsPersisted() throws IOException {
        EmailDeadLetterRepository repository = mock(EmailDeadLetterRepository.class);
        EmailDeadLetterConsumer consumer = new EmailDeadLetterConsumer(repository);
        Channel channel = mock(Channel.class);
        EmailNotificationMessage message = message();

        consumer.capture(message, channel, 17L);

        verify(repository).capture(message);
        verify(channel).basicAck(17L, false);
    }

    @Test
    void requeuesMessageWhenDurablePersistenceFails() throws IOException {
        EmailDeadLetterRepository repository = mock(EmailDeadLetterRepository.class);
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(repository).capture(org.mockito.ArgumentMatchers.any());
        EmailDeadLetterConsumer consumer = new EmailDeadLetterConsumer(repository);
        Channel channel = mock(Channel.class);
        Logger logger = (Logger) LoggerFactory.getLogger(EmailDeadLetterConsumer.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);

        try {
            consumer.capture(message(), channel, 23L);
        } finally {
            logger.setLevel(previousLevel);
        }

        verify(channel).basicNack(23L, false, true);
    }

    private EmailNotificationMessage message() {
        return new EmailNotificationMessage(
                UUID.randomUUID(), "ORDER_CREATED", 1, 23L, Instant.now(), Map.of("orderId", 23L));
    }
}
