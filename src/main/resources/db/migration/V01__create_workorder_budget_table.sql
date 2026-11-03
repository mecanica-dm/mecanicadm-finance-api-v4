CREATE TABLE IF NOT EXISTS work_order_budgets (
    work_order_id UUID PRIMARY KEY,
    total_price DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    observation TEXT
);

CREATE INDEX IF NOT EXISTS idx_work_order_budgets_work_order_id ON work_order_budgets(work_order_id);
