CREATE TABLE IF NOT EXISTS payments
(
    id                     UUID           NOT NULL PRIMARY KEY,
    external_reference     VARCHAR(64)    NOT NULL,
    idempotency_key        VARCHAR(128)   NOT NULL,
    mercado_pago_order_id  VARCHAR(64),
    mercado_pago_payment_id VARCHAR(64),
    status                 VARCHAR(32)    NOT NULL,
    status_detail          VARCHAR(128),
    amount                 NUMERIC(19, 2) NOT NULL,
    installments           INTEGER        NOT NULL,
    payment_method_id      VARCHAR(32),
    payer_email            VARCHAR(255),
    provider_response      JSONB,
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_payments_external_reference ON payments (external_reference);
CREATE INDEX IF NOT EXISTS idx_payments_mercado_pago_order_id ON payments (mercado_pago_order_id);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments (created_at);
