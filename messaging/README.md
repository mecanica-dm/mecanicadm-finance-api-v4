# RabbitMQ

O arquivo `definitions.json` contém a topologia importável no RabbitMQ (exchanges, filas e bindings).
A aplicação **não declara** mais esses recursos em tempo de execução.

## Conteúdo

| Recurso   | Tipo      | Nome         |
|-----------|-----------|--------------|
| Exchange  | `headers` | `finance-output` |
| Exchange  | `headers` | `finance-input`  |
| Exchange  | `direct`  | `finance-dlx`    |
| Fila      | —         | `budget`         |
| Fila      | —         | `budget.dlq`     |

A `finance-output` é usada apenas para eventos **publicados** pela aplicação.

Bindings da `finance-input`, usada apenas para eventos **consumidos** (header `event-type`, `x-match = all`):

- `GenerateBudgetIntegrationCommand`
- `CancelBudgetIntegrationCommand`
- `WorkOrderCreatedEvent`

## Como importar

Pela interface de gerenciamento (`http://localhost:15672`):

1. Acesse **Admin → Import definitions**.
2. Selecione `docker/rabbitmq/definitions.json`.
3. Clique em **Import**.

Via CLI:

```bash
rabbitmqctl import_definitions /caminho/para/definitions.json
```

Ou via HTTP:

```bash
curl -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST \
  --data @docker/rabbitmq/definitions.json \
  http://localhost:15672/api/definitions
```

> Os nomes no JSON devem corresponder às variáveis `RABBITMQ_EXCHANGE_OUTPUT`,
> `RABBITMQ_EXCHANGE_INPUT`, `RABBITMQ_QUEUE_BUDGET`, `RABBITMQ_EXCHANGE_DEAD_LETTER` e
> `RABBITMQ_QUEUE_BUDGET_DLQ` usadas pela aplicação.
