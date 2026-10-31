package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.response.EmailDeadLetterSummary;
import com.ecommerce.platform.service.EmailDeadLetterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailDeadLetterControllerTest {

    private EmailDeadLetterService deadLetterService;
    private EmailDeadLetterController controller;

    @BeforeEach
    void setUp() {
        deadLetterService = mock(EmailDeadLetterService.class);
        controller = new EmailDeadLetterController(deadLetterService);
    }

    @Test
    void listsDeadLettersUsingRequestedLimit() {
        List<EmailDeadLetterSummary> deadLetters = List.of(
                new EmailDeadLetterSummary(UUID.randomUUID(), "ORDER_CREATED",
                        Instant.now(), Instant.now(), 0, "OPEN"));
        when(deadLetterService.list(25)).thenReturn(deadLetters);

        assertThat(controller.list(25).getBody()).isSameAs(deadLetters);
        verify(deadLetterService).list(25);
    }

    @Test
    void queuesReplayAndReturnsAccepted() {
        UUID eventId = UUID.randomUUID();

        var response = controller.replay(eventId);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(deadLetterService).replay(eventId);
    }
}
