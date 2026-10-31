package com.ecommerce.platform.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public void enqueue(
            UUID eventId,
            String eventType,
            String aggregateType,
            Long aggregateId,
            String payload) {
        jdbcTemplate.update("""
                INSERT INTO outbox_events
                    (event_id, event_type, schema_version, aggregate_type, aggregate_id, payload)
                VALUES (?, ?, 1, ?, ?, CAST(? AS jsonb))
                """, eventId, eventType, aggregateType, aggregateId, payload);
    }

    public List<OutboxRecord> claimBatch(int batchSize, int leaseSeconds) {
        return jdbcTemplate.query("""
                WITH candidates AS (
                    SELECT event_id
                    FROM outbox_events
                    WHERE (status = 'PENDING' AND next_attempt_at <= now())
                       OR (status = 'PROCESSING' AND lease_until < now())
                    ORDER BY created_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_events AS event
                SET status = 'PROCESSING',
                    lease_until = now() + (? * interval '1 second'),
                    attempt_count = attempt_count + 1,
                    updated_at = now()
                FROM candidates
                WHERE event.event_id = candidates.event_id
                RETURNING event.event_id, event.event_type, event.schema_version,
                          event.aggregate_id, event.created_at, event.payload::text,
                          event.attempt_count
                """, this::mapRecord, batchSize, leaseSeconds);
    }

    public void markPublished(UUID eventId) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'PUBLISHED', published_at = now(), lease_until = NULL,
                    last_error = NULL, updated_at = now()
                WHERE event_id = ?
                """, eventId);
    }

    public void markFailed(UUID eventId, int attemptCount, int maxAttempts, String error) {
        if (attemptCount >= maxAttempts) {
            jdbcTemplate.update("""
                    UPDATE outbox_events
                    SET status = 'DEAD', lease_until = NULL, last_error = ?, updated_at = now()
                    WHERE event_id = ?
                    """, truncate(error), eventId);
            return;
        }

        long delaySeconds = Math.min(300, 1L << Math.min(attemptCount, 8));
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'PENDING',
                    next_attempt_at = now() + (? * interval '1 second'),
                    lease_until = NULL, last_error = ?, updated_at = now()
                WHERE event_id = ?
                """, delaySeconds, truncate(error), eventId);
    }

    private OutboxRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxRecord(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getInt("schema_version"),
                resultSet.getLong("aggregate_id"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getString("payload"),
                resultSet.getInt("attempt_count"));
    }

    private String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
