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

A importação é **automática**: o `.github/workflows/app-deploy.yml` roda o passo
`Importa a topologia do RabbitMQ (CloudAMQP)` a cada deploy (e sempre que
`messaging/**` muda). Ele lê o host do ConfigMap `mecanicadm-finance-config` e as
credenciais do SSM (`/mecanicadm/prod/rabbitmq/*`), e antes do `POST` ajusta o
JSON para o plano compartilhado:

1. Troca todos os `"vhost": "/"` pelo vhost real da instância (o plano
   compartilhado não usa `/`).
2. Remove as seções `vhosts`, `users`, `parameters`, `global_parameters` e
   `policies` — o plano compartilhado não permite gerenciá-los.

A importação é idempotente e não remove recursos de outros serviços que
compartilham o vhost (ex.: as filas do workorder). O import manual abaixo só é
necessário em uma emergência (ex.: rodar fora da pipeline).

```bash
jq --arg vhost "$RABBITMQ_VHOST" \
  'del(.vhosts, .users, .parameters, .global_parameters, .policies)
   | (.exchanges[]?.vhost, .queues[]?.vhost, .bindings[]?.vhost) = $vhost' \
  messaging/definitions.json > /tmp/definitions.json

curl -u "$RABBITMQ_USER:$RABBITMQ_PASSWORD" \
  -H "Content-Type: application/json" \
  -X POST --data @/tmp/definitions.json \
  "https://$RABBITMQ_HOST/api/definitions"
```

A aplicação **não** declara a topologia em runtime: se uma fila não existir, o
listener não consome e fica em retry.
