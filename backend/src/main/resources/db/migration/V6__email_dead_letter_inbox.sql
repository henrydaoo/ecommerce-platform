CREATE TABLE email_dead_letters (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    message JSONB NOT NULL,
    first_failed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_failed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    replay_count INTEGER NOT NULL DEFAULT 0,
    replay_state VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    replay_claimed_at TIMESTAMPTZ,
    last_replayed_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_email_dead_letter_replay_count CHECK (replay_count >= 0),
    CONSTRAINT chk_email_dead_letter_state
        CHECK (replay_state IN ('OPEN', 'PUBLISHING', 'REPLAY_QUEUED', 'RESOLVED'))
);

CREATE INDEX idx_email_dead_letters_open
    ON email_dead_letters (last_failed_at DESC)
    WHERE replay_state <> 'RESOLVED';
