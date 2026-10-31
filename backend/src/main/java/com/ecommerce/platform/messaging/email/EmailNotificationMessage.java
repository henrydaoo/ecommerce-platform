package com.ecommerce.platform.messaging.email;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EmailNotificationMessage(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Long aggregateId,
        Instant occurredAt,
        Map<String, Object> payload) {
}
