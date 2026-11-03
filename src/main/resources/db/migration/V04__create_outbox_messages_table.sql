CREATE TABLE outbox_messages
(
    id           UUID         NOT NULL PRIMARY KEY,
    event_type   VARCHAR(255) NOT NULL,
    aggregate_id UUID,
    payload      JSONB        NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    last_error   TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    CONSTRAINT ck_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_pending
    ON outbox_messages (created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_processing
    ON outbox_messages (updated_at)
    WHERE status = 'PROCESSING';

CREATE INDEX idx_outbox_published
    ON outbox_messages (published_at)
    WHERE status = 'PUBLISHED';

CREATE INDEX idx_outbox_failed
    ON outbox_messages (updated_at)
    WHERE status = 'FAILED';

CREATE INDEX idx_outbox_aggregate
    ON outbox_messages (aggregate_id, created_at);
