package com.ecommerce.platform.messaging.email;

import com.ecommerce.platform.mail.MailService;
import com.ecommerce.platform.service.EmailDeadLetterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationConsumerTest {

    @Test
    void sendsWelcomeNotificationAndStoresDeduplicationMarker() {
        MailService mailService = mock(MailService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EmailDeadLetterService deadLetterService = mock(EmailDeadLetterService.class);
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(eventId))).thenReturn(false);
        EmailNotificationConsumer consumer = new EmailNotificationConsumer(
                mailService, jdbcTemplate, new ObjectMapper(), new SimpleMeterRegistry(), deadLetterService);
        EmailNotificationMessage message = new EmailNotificationMessage(
                eventId, "USER_REGISTERED", 1, 12L, Instant.now(), Map.of("userId", 12L));

        consumer.consume(message);

        verify(mailService).sendWelcomeEmail(12L);
        verify(jdbcTemplate).update(anyString(), eq(eventId), eq("USER_REGISTERED"));
        verify(deadLetterService).markResolved(eventId);
    }

    @Test
    void skipsAnAlreadyDeliveredEvent() {
        MailService mailService = mock(MailService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EmailDeadLetterService deadLetterService = mock(EmailDeadLetterService.class);
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(eventId))).thenReturn(true);
        EmailNotificationConsumer consumer = new EmailNotificationConsumer(
                mailService, jdbcTemplate, new ObjectMapper(), new SimpleMeterRegistry(), deadLetterService);
        EmailNotificationMessage message = new EmailNotificationMessage(
                eventId, "USER_REGISTERED", 1, 12L, Instant.now(), Map.of("userId", 12L));

        consumer.consume(message);

        verify(mailService, never()).sendWelcomeEmail(any());
        verify(jdbcTemplate, never()).update(anyString(), any(), any());
        verify(deadLetterService).markResolved(eventId);
    }

    @Test
    void rejectsUnknownEventTypesForDeadLetterHandling() {
        EmailNotificationConsumer consumer = new EmailNotificationConsumer(
                mock(MailService.class), mock(JdbcTemplate.class),
                new ObjectMapper(), new SimpleMeterRegistry(), mock(EmailDeadLetterService.class));
        EmailNotificationMessage message = new EmailNotificationMessage(
                UUID.randomUUID(), "UNKNOWN", 1, 12L, Instant.now(), Map.of());

        assertThrows(IllegalArgumentException.class, () -> consumer.consume(message));
    }

    @Test
    void rejectsUnsupportedSchemaVersions() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EmailNotificationConsumer consumer = new EmailNotificationConsumer(
                mock(MailService.class), jdbcTemplate, new ObjectMapper(), new SimpleMeterRegistry(),
                mock(EmailDeadLetterService.class));
        EmailNotificationMessage message = new EmailNotificationMessage(
                UUID.randomUUID(), "USER_REGISTERED", 2, 12L, Instant.now(), Map.of("userId", 12L));

        assertThrows(IllegalArgumentException.class, () -> consumer.consume(message));
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Boolean.class), any());
    }
}
