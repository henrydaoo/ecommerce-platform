CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    lease_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'DEAD')),
    CONSTRAINT chk_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT chk_outbox_schema_version CHECK (schema_version > 0)
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_events_expired_lease
    ON outbox_events (lease_until)
    WHERE status = 'PROCESSING';

CREATE TABLE email_notification_deliveries (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    delivered_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
