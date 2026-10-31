package com.ecommerce.platform.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EmailDeadLetterSummary(
        UUID eventId,
        String eventType,
        Instant firstFailedAt,
        Instant lastFailedAt,
        int replayCount,
        String replayState) {
}
