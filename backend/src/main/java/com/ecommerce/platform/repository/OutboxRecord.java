package com.ecommerce.platform.repository;

import java.util.UUID;
import java.time.Instant;

public record OutboxRecord(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Long aggregateId,
        Instant occurredAt,
        String payload,
        int attemptCount) {
}
