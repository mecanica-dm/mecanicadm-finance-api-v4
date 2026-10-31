-- =============================================================
-- Seed de dados - mecanicadm-finance-api-v4
-- -------------------------------------------------------------
-- Este script apenas popula o banco. O schema (tabelas, colunas,
-- indices e chaves estrangeiras) e de responsabilidade exclusiva
-- das migrations do Flyway em src/main/resources/db/migration:
--
--   V01 -> work_order_budgets
--   V02 -> budget_decision_tokens
--   V03 -> work_order_budget_info (coluna JSONB em work_order_budgets)
--
-- Apenas as tabelas do proprio servico sao semeadas aqui. Os demais
-- dominios (client, labor, material, vehicle, workorder) pertencem
-- aos seus proprios servicos/migrations e nao sao semeados aqui.
--
-- Requisitos:
--   - as migrations precisam ter rodado antes deste script;
--   - o script e idempotente (pode ser executado quantas vezes for preciso).
-- =============================================================

\set ON_ERROR_STOP on

BEGIN;

-- =========================================
-- 1. INSERIR ORÇAMENTOS
-- =========================================
-- status (WorkOrderBudgetStatus): PENDING, WAITING_DECISION, APPROVED,
--                                  REJECTED, CHANGES_REQUESTED, CANCELED
INSERT INTO work_order_budgets (work_order_id, total_price, status, observation) VALUES
('aa11aa11-1111-4aa1-8a11-aaaaaaaa1111', 325.00, 'APPROVED', 'Aprovado pelo cliente via link de decisão'),
('cc33cc33-3333-4cc3-8c33-cccccccccccc', 1080.00, 'PENDING', null),
('dd44dd44-4444-4dd4-8d44-dddddddddddd', 335.00, 'WAITING_DECISION', 'Ajuste manual de R$ 200,00 aplicado sobre o valor calculado'),
('ee55ee55-5555-4ee5-8e55-eeeeeeeeeeee', 330.00, 'PENDING', null),
('ff66ff66-6666-4ff6-8f66-ffffffff6666', 1080.00, 'APPROVED', 'Aprovado pelo cliente via link de decisão'),
('66776677-7777-4667-8677-666666667777', 600.00, 'APPROVED', 'Aprovado pelo cliente via link de decisão'),
('88886688-8888-4888-8888-888888888888', 175.00, 'APPROVED', 'Ajuste manual de R$ 85,00 aplicado sobre o valor calculado')
ON CONFLICT DO NOTHING;

-- =========================================
-- 2. INSERIR TOKENS DE DECISÃO DO ORÇAMENTO
-- =========================================
-- Token de decisão do orçamento da work order dd44dd44-4444-4dd4-8d44-dddddddddddd
-- (válido por 24h a partir da criação).
INSERT INTO budget_decision_tokens (id, work_order_id, token, used, created_at) VALUES
('dd44dd44-9999-4dd4-8d44-dddddddddddd', 'dd44dd44-4444-4dd4-8d44-dddddddddddd', 'b7f3c9a1-5d2e-4a6b-9c8d-1e2f3a4b5c6d', false, now())
ON CONFLICT DO NOTHING;

COMMIT;
