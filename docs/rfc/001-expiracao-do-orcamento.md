# RFC 001 - Expiração do Orçamento

* **Status**: Aprovado (implementado em 12/11/2026)
* **Data**: 12/11/2026
* **Autores**: Time de desenvolvimento
* **Resumo**: Define como o Finance **publica o `BudgetExpiredEvent`** quando um orçamento enviado ao cliente não é
  decidido a tempo, para a saga cancelar a OS.

---

## 1. Resumo

A saga trata a expiração: ao receber o `BudgetExpiredEvent` com a OS aguardando a decisão do cliente, ela pede o
cancelamento da OS. Faltava quem **publicasse** esse evento no Finance: a única "expiração" era o link do e-mail, que deixa
de valer depois de 24 horas. Esta RFC descreve a rotina que o publica.

## 2. Contexto (antes da rotina)

- O orçamento enviado fica em `WAITING_DECISION`, e o cliente decide por um link de uso único, válido por 24 horas
  ([ADR 006](../adr/006-rota-publica-de-decisao-do-orcamento.md)).
- Se o cliente não decidia, o link expirava, o orçamento continuava em `WAITING_DECISION` e a saga em
  `AWAITING_CUSTOMER_DECISION`, **para sempre**. A OS não andava nem era cancelada.
- Os cenários 5 e 8 do desafio (rejeição e expiração) só se completavam pela rejeição explícita.

## 3. Objetivos

- Detectar o orçamento cujo prazo de decisão acabou e avisar a saga **uma única vez**.
- Funcionar com mais de uma instância do Finance sem publicar o evento em duplicidade.
- Não mudar o contrato com a saga.

## 4. Alternativas Consideradas

### 4.1. Rotina agendada que procura orçamentos vencidos

Uma tarefa periódica seleciona os orçamentos em `WAITING_DECISION` cujo token mais recente já expirou e não foi usado,
publica o `BudgetExpiredEvent` e registra que o evento foi emitido.

**Prós:** simples, usa o que já existe (a tabela de tokens e a outbox) e é fácil de testar; padrão já usado na limpeza do
inbox e da outbox.

**Contras:** o aviso chega com a latência do intervalo da rotina; exige marcar o orçamento para não repetir o evento.

### 4.2. Mensagem com atraso no RabbitMQ (TTL e DLX)

Ao enviar o orçamento, publicar uma mensagem que só é entregue depois de 24 horas.

**Prós:** sem polling no banco.

**Contras:** mais infraestrutura (fila de espera e dead-letter); se o orçamento for decidido antes, a mensagem ainda chega e
precisa ser descartada; difícil de testar.

### 4.3. Verificar só quando alguém consultar o orçamento

**Prós:** nenhuma rotina nova.

**Contras:** se ninguém consultar, nada acontece; era o comportamento anterior.

## 5. Solução adotada

Implementada a **alternativa 4.1**:

| Aspecto | Solução |
|---|---|
| Rotina | `BudgetExpirationJob`, a cada minuto por padrão (`BUDGET_EXPIRATION_INTERVAL_MS`), com o caso de uso `ExpireWorkOrderBudgetsUseCase` |
| Seleção | Token **não usado**, **ainda não avisado**, vencido (mais de 24 horas), que seja o **mais recente** da OS, de um orçamento em `WAITING_DECISION` |
| Efeito | Publica `BudgetExpiredEvent` pela outbox e marca o token como avisado (`budget_decision_tokens.expiry_notified`), na mesma transação |
| Concorrência | Seleção com `FOR UPDATE SKIP LOCKED`, como na outbox, para várias instâncias |
| Cancelamento | O Finance **não cancela** o orçamento sozinho: a saga, ao tratar a expiração, envia o `CancelBudgetIntegrationCommand` e o Finance responde com `BudgetCanceledEvent` |
| Configuração | `BUDGET_EXPIRATION_ENABLED`, `BUDGET_EXPIRATION_INITIAL_DELAY_MS`, `BUDGET_EXPIRATION_INTERVAL_MS` e `BUDGET_EXPIRATION_BATCH_SIZE` |

O prazo continua o do token (24 horas), então a rotina só confirma o que o link já diz.

## 6. Impacto e Riscos

- **Risco**: publicar o evento para um orçamento que o cliente decidiu no limite do prazo.
    - **Mitigação**: a seleção e a decisão por token usam o mesmo critério e a mesma transação de gravação; a saga ignora a
      expiração que chegue fora de `AWAITING_CUSTOMER_DECISION`.
- **Risco**: evento repetido.
    - **Mitigação**: a data do aviso impede nova seleção, e o inbox da saga descarta duplicatas.

## 7. Implementação

1. Migration V07, com a coluna `expiry_notified` e um índice parcial dos tokens ainda não avisados. (feito)
2. `JdbcBudgetExpiryGateway` (consulta com `SKIP LOCKED`), `ExpireWorkOrderBudgetsUseCase` e `BudgetExpirationJob`. (feito)
3. Publicar o `BudgetExpiredEvent` pela outbox. (feito)
4. Testes: `ExpireWorkOrderBudgetsUseCaseTest`, `BudgetExpirationJobTest` e `JdbcBudgetExpiryGatewayIT` (token vencido, ainda válido, usado, substituído por outro mais novo, orçamento já decidido, aviso único e limite do lote). (feito)

## 8. Decisão Final

| Item       | Detalhe                                |
|------------|----------------------------------------|
| Expiração  | Rotina agendada que publica o evento   |
| Status     | Aprovada e implementada                |
