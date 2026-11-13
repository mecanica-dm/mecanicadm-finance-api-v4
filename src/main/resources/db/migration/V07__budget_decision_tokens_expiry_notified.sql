ALTER TABLE budget_decision_tokens
    ADD COLUMN IF NOT EXISTS expiry_notified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_budget_decision_tokens_expiry_pending
    ON budget_decision_tokens (created_at)
    WHERE used = FALSE AND expiry_notified = FALSE;
