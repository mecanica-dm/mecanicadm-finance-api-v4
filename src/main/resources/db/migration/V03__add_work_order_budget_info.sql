ALTER TABLE work_order_budgets
    ADD COLUMN IF NOT EXISTS work_order_budget_info JSONB;

-- Existing rows may have been created with the legacy array default ('[]'::jsonb).
-- The domain expects an object-shaped payload, so normalize every non-object value.
UPDATE work_order_budgets
SET work_order_budget_info = '{}'::jsonb
WHERE work_order_budget_info IS NULL
   OR jsonb_typeof(work_order_budget_info) <> 'object';

ALTER TABLE work_order_budgets
    ALTER COLUMN work_order_budget_info SET DEFAULT '{}'::jsonb,
    ALTER COLUMN work_order_budget_info SET NOT NULL;
