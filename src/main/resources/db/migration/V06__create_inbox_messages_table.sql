CREATE TABLE inbox_messages
(
    message_id   UUID         NOT NULL,
    consumer     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    aggregate_id UUID,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    received_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    last_error   TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_inbox_messages PRIMARY KEY (message_id, consumer),
    CONSTRAINT ck_inbox_status
        CHECK (status IN ('PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_inbox_processed_at
    ON inbox_messages (processed_at)
    WHERE status = 'PROCESSED';

CREATE INDEX idx_inbox_aggregate
    ON inbox_messages (aggregate_id, received_at);
