package com.ecommerce.platform.messaging.dlq;

import com.ecommerce.platform.messaging.email.EmailNotificationMessage;
import com.ecommerce.platform.repository.EmailDeadLetterRepository;
import com.ecommerce.platform.service.EmailDeadLetterService;
import com.ecommerce.platform.service.impl.EmailDeadLetterServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailDeadLetterReplayTest {

    @Test
    void republishesOnlyAfterItSuccessfullyClaimsTheDeadLetter() {
        EmailDeadLetterRepository repository = mock(EmailDeadLetterRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        EmailDeadLetterService service = service(repository, rabbitTemplate);
        UUID eventId = UUID.randomUUID();
        EmailNotificationMessage message = message(eventId);
        when(repository.findMessage(eventId)).thenReturn(Optional.of(message));
        when(repository.claimReplay(eventId)).thenReturn(true);
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(EmailNotificationMessage.class), any(CorrelationData.class));

        service.replay(eventId);

        verify(repository).markReplayQueued(eventId);
    }

    @Test
    void rejectsConcurrentReplayWithoutPublishingAgain() {
        EmailDeadLetterRepository repository = mock(EmailDeadLetterRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        EmailDeadLetterService service = service(repository, rabbitTemplate);
        UUID eventId = UUID.randomUUID();
        when(repository.findMessage(eventId)).thenReturn(Optional.of(message(eventId)));
        when(repository.claimReplay(eventId)).thenReturn(false);

        assertThatThrownBy(() -> service.replay(eventId))
                .isInstanceOf(ResponseStatusException.class);
        verify(rabbitTemplate, never()).convertAndSend(
                anyString(), anyString(), any(EmailNotificationMessage.class), any(CorrelationData.class));
    }

    private EmailDeadLetterService service(
            EmailDeadLetterRepository repository,
            RabbitTemplate rabbitTemplate) {
        EmailDeadLetterServiceImpl implementation =
                new EmailDeadLetterServiceImpl(repository, rabbitTemplate);
        ReflectionTestUtils.setField(implementation, "exchange", "email.exchange");
        ReflectionTestUtils.setField(implementation, "routingKey", "email.route");
        return implementation;
    }

    private EmailNotificationMessage message(UUID eventId) {
        return new EmailNotificationMessage(
                eventId, "ORDER_CREATED", 1, 23L, Instant.now(), Map.of("orderId", 23L));
    }
}
