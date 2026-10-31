package com.ecommerce.platform.repository;

import com.ecommerce.platform.messaging.email.EmailNotificationMessage;
import com.ecommerce.platform.dto.response.EmailDeadLetterSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmailDeadLetterRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void capture(EmailNotificationMessage message) throws JsonProcessingException {
        if (Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM email_notification_deliveries WHERE event_id = ?)",
                Boolean.class, message.eventId()))) {
            markResolved(message.eventId());
            return;
        }

        String json = objectMapper.writeValueAsString(message);
        jdbcTemplate.update("""
                INSERT INTO email_dead_letters (event_id, event_type, message)
                VALUES (?, ?, CAST(? AS jsonb))
                ON CONFLICT (event_id) DO UPDATE
                SET last_failed_at = now(),
                    replay_state = 'OPEN',
                    replay_claimed_at = NULL,
                    resolved_at = NULL
                """, message.eventId(), message.eventType(), json);
    }

    public List<EmailDeadLetterSummary> findUnresolved(int limit) {
        return jdbcTemplate.query("""
                SELECT event_id, event_type, first_failed_at, last_failed_at,
                       replay_count, replay_state
                FROM email_dead_letters
                WHERE replay_state <> 'RESOLVED'
                ORDER BY last_failed_at DESC
                LIMIT ?
                """, this::mapSummary, limit);
    }

    public Optional<EmailNotificationMessage> findMessage(UUID eventId) {
        List<String> messages = jdbcTemplate.query("""
                SELECT message::text
                FROM email_dead_letters
                WHERE event_id = ? AND replay_state <> 'RESOLVED'
                """, (resultSet, rowNumber) -> resultSet.getString(1), eventId);
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(messages.get(0), EmailNotificationMessage.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read stored email dead letter " + eventId, exception);
        }
    }

    public boolean claimReplay(UUID eventId) {
        return jdbcTemplate.update("""
                UPDATE email_dead_letters
                SET replay_state = 'PUBLISHING',
                    replay_claimed_at = now(),
                    replay_count = replay_count + 1
                WHERE event_id = ? AND replay_state = 'OPEN'
                """, eventId) == 1;
    }

    public void markReplayQueued(UUID eventId) {
        jdbcTemplate.update("""
                UPDATE email_dead_letters
                SET replay_state = 'REPLAY_QUEUED',
                    replay_claimed_at = NULL,
                    last_replayed_at = now()
                WHERE event_id = ? AND replay_state = 'PUBLISHING'
                """, eventId);
    }

    public void releaseReplayClaim(UUID eventId) {
        jdbcTemplate.update("""
                UPDATE email_dead_letters
                SET replay_state = 'OPEN', replay_claimed_at = NULL
                WHERE event_id = ? AND replay_state = 'PUBLISHING'
                """, eventId);
    }

    public int releaseStaleReplayClaims() {
        return jdbcTemplate.update("""
                UPDATE email_dead_letters
                SET replay_state = 'OPEN', replay_claimed_at = NULL
                WHERE replay_state = 'PUBLISHING'
                  AND replay_claimed_at < now() - interval '5 minutes'
                """);
    }

    public void markResolved(UUID eventId) {
        jdbcTemplate.update("""
                UPDATE email_dead_letters
                SET replay_state = 'RESOLVED', replay_claimed_at = NULL,
                    resolved_at = now()
                WHERE event_id = ?
                """, eventId);
    }

    private EmailDeadLetterSummary mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EmailDeadLetterSummary(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getTimestamp("first_failed_at").toInstant(),
                resultSet.getTimestamp("last_failed_at").toInstant(),
                resultSet.getInt("replay_count"),
                resultSet.getString("replay_state"));
    }
}
