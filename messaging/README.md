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
2. Selecione `messaging/definitions.json`.
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
  --data @messaging/definitions.json \
  http://localhost:15672/api/definitions
```

> Os nomes no JSON devem corresponder às variáveis `RABBITMQ_EXCHANGE_OUTPUT`,
> `RABBITMQ_EXCHANGE_INPUT`, `RABBITMQ_QUEUE_BUDGET`, `RABBITMQ_EXCHANGE_DEAD_LETTER`,
> `RABBITMQ_QUEUE_BUDGET_DLQ`, `RABBITMQ_QUEUE_PAYMENT` e `RABBITMQ_QUEUE_PAYMENT_DLQ`
> usadas pela aplicação.

## Produção (CloudAMQP)

O broker de `prod` é uma instância compartilhada do CloudAMQP, acessada por
`amqps` na porta `5671`.

Nesse plano o `POST /api/definitions` **não funciona**: o endpoint exige a tag
`administrator` no RabbitMQ 4.x, e o usuário da instância tem apenas
`management,policymaker` (o CloudAMQP documenta a importação de definitions
como recurso exclusivo de instâncias dedicadas). Por isso a topologia é aplicada
**recurso por recurso** (`PUT /api/exchanges`, `PUT /api/queues`,
`POST /api/bindings`), que usam só as permissões normais de vhost.

A aplicação é **automática**: o `.github/workflows/app-deploy.yml` roda o passo
`Aplica a topologia do RabbitMQ (CloudAMQP)` a cada deploy (e sempre que
`messaging/**` muda). Ele lê o host do ConfigMap `mecanicadm-finance-config` e as
credenciais do SSM (`/mecanicadm/prod/rabbitmq/*`) e chama
`devops/scripts/rabbitmq-apply-topology.sh`.

Para rodar manualmente (emergência, fora da pipeline):

```bash
RABBITMQ_HOST="$RABBITMQ_HOST" \
RABBITMQ_USER="$RABBITMQ_USER" \
RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
RABBITMQ_VHOST="$RABBITMQ_VHOST" \
  devops/scripts/rabbitmq-apply-topology.sh messaging/definitions.json
```

O script é idempotente (rodar de novo não duplica exchanges, filas nem
bindings), falha em caso de drift (mesma fila com `x-dead-letter-*` diferente) e
não remove recursos de outros serviços no mesmo vhost — as filas do workorder
continuam intactas.

A aplicação **não** declara a topologia em runtime: se uma fila não existir, o
listener não consome e fica em retry.
