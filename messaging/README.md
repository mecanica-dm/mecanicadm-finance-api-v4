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
| Fila      | —         | `payment`        |
| Fila      | —         | `payment.dlq`    |

A `finance-output` é usada apenas para eventos **publicados** pela aplicação.

Bindings da `finance-input`, usada apenas para eventos **consumidos** (header `event-type`, `x-match = all`):

- `GenerateBudgetIntegrationCommand`
- `CancelBudgetIntegrationCommand`
- `GeneratePaymentIntegrationCommand` (fila `payment`)

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
> `RABBITMQ_EXCHANGE_INPUT`, `RABBITMQ_QUEUE_BUDGET`, `RABBITMQ_EXCHANGE_DEAD_LETTER`,
> `RABBITMQ_QUEUE_BUDGET_DLQ`, `RABBITMQ_QUEUE_PAYMENT` e `RABBITMQ_QUEUE_PAYMENT_DLQ`
> usadas pela aplicação.

## Produção (CloudAMQP)

O broker de `prod` é uma instância compartilhada do CloudAMQP, acessada por
`amqps` na porta `5671`. Antes de importar `definitions.json`:

1. Troque todos os `"vhost": "/"` pelo vhost real da instância (o plano
   compartilhado não usa `/`).
2. Remova as seções `vhosts`, `users`, `parameters` e `global_parameters` — o
   plano compartilhado não permite gerenciar usuários nem vhosts.
3. Importe via `POST https://<host>/api/definitions` ou crie os recursos um a
   um na Management UI (`https://<host>/`).

A aplicação **não** declara a topologia em runtime: se uma fila não existir, o
listener não consome e fica em retry. Importe a topologia antes do deploy.
