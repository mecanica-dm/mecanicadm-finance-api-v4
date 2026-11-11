# [003]: [Estratégia de Inbox — consumo idempotente de mensagens]

* **Status**: Aceito
* **Data**: 05/11/2026

## 1. Contexto e Problema

A publicação de eventos já utiliza o padrão **Transactional Outbox** (`outbox_messages`), garantindo entrega
*at-least-once*: uma mensagem pode ser publicada mais de uma vez caso o relay falhe entre o envio ao RabbitMQ e a
marcação como `PUBLISHED`.

No consumo, o serviço ainda não possui mecanismo de deduplicação. Isso permite que a mesma mensagem seja processada
mais de uma vez quando:

- o produtor reenvia a mensagem;
- o RabbitMQ reentrega após falha de `ack`;
- a aplicação cai depois do commit do banco e antes do `ack`;
- duas instâncias consomem a mesma mensagem simultaneamente.

Hoje o impacto é real em `GenerateBudgetIntegrationCommand`: o `CreateWorkOrderBudgetUseCase` regrava o orçamento e publica um
novo `BudgetGeneratedEvent`, gerando eventos duplicados para os demais serviços. `CalculateWorkOrderBudgetUseCase`
possui o mesmo comportamento.

Além disso, os listeners atuais capturam `Exception` e apenas registram log:

```java
try {
    useCase.execute(command);
} catch (Exception e) {
    logger.error("Erro ao processar ...", e);
}
```

Esse comportamento descarta a mensagem (o container dá `ack`) e impediria uma inbox de funcionar corretamente: a
transação de deduplicação seria confirmada mesmo com a regra de negócio falhando.

## 2. Decisão

Adotar o padrão **Transactional Inbox** (consumidor idempotente) com as seguintes características:

| Item | Decisão |
|------|---------|
| Chave de deduplicação | `messageId` da mensagem (`MessageProperties.messageId` do AMQP) |
| Chave de fallback | Hash determinístico de `eventType + body` quando o AMQP não trouxer `messageId` |
| Identidade do consumidor | Nome da fila (`app.rabbitmq.queues.budget`) |
| Chave primária | `(message_id, consumer)` |
| Momento da gravação | Mesma transação de banco da regra de negócio |
| Concorrência | `INSERT ... ON CONFLICT DO NOTHING` + chave primária |
| Falha no processamento | Rollback da transação remove o registro da inbox; mensagem é reentregue |
| Retenção | Limpeza periódica de registros `PROCESSED` antigos |
| Entrega | Mantém *at-least-once*; o efeito no banco passa a ser *effectively-once* |

### 2.1 Por que gravar na mesma transação

O ponto central da estratégia é que o registro da inbox e o efeito de negócio sejam atômicos:

1. A mensagem é recebida.
2. A transação de banco é iniciada.
3. É executado `INSERT INTO inbox_messages ... ON CONFLICT DO NOTHING`.
   - Se já existir, a mensagem é duplicada: a transação é encerrada sem executar a regra de negócio.
   - Se não existir, o registro entra como `PROCESSING` e a regra de negócio é executada.
4. A regra de negócio grava os dados e o evento na outbox, na mesma transação.
5. O registro da inbox é atualizado para `PROCESSED`.
6. A transação é confirmada.
7. O container do RabbitMQ dá `ack` na mensagem.

Se qualquer etapa falhar, o rollback remove o registro da inbox e a mensagem pode ser reentregue. Se o commit
funcionar e o `ack` falhar, o RabbitMQ reentrega a mensagem, mas a inbox identifica a duplicidade e não executa a
regra novamente.

> **Importante:** a deduplicação só é segura se o processamento e o registro da inbox estiverem na mesma transação
> (`Propagation.REQUIRED`). Uma transação `REQUIRES_NEW` para a inbox criaria janelas de perda ou duplicidade.

### 2.2 Concorrência

Duas entregas simultâneas da mesma mensagem disputam a chave primária:

- A primeira transação insere e segue com o processamento.
- A segunda fica bloqueada no índice único até a primeira terminar.
- Se a primeira confirmar, o `ON CONFLICT DO NOTHING` da segunda não insere nada e o processamento é ignorado.
- Se a primeira falhar, o rollback libera a chave e a segunda processa normalmente.

## 3. Estrutura na Clean Architecture

A solução segue a separação já usada no projeto: regra de aplicação no `core`, detalhes de infraestrutura em `infra`.

```text
src/main/java/com/mecanicadm/mecanicadm_api/
├── core/
│   └── shared/
│       ├── domain/
│       │   ├── InboxEntry.java                    # modelo de entrada da inbox (novo)
│       │   └── port/
│       │       └── InboxGateway.java              # porta de persistência (novo)
│       └── usecase/
│           └── ProcessInboundMessageUseCase.java  # orquestração transacional (novo)
├── infra/
│   ├── messaging/
│   │   ├── inbox/
│   │   │   ├── JdbcInboxGateway.java              # adaptador JDBC (novo)
│   │   │   ├── InboxProperties.java               # configuração app.inbox (novo)
│   │   │   ├── InboxConfiguration.java            # beans e métricas (novo)
│   │   │   └── InboxCleanupJob.java               # limpeza agendada (novo)
│   │   └── rabbit/
│   │       └── RabbitConfig.java                  # DLQ e retry (alterar)
│   └── features/workorder/messaging/
│       ├── BudgetQueueListener.java               # resolve messageId e usa a inbox (alterar)
│       ├── GenerateBudgetIntegrationListener.java # remover catch que engole exceção (alterar)
│       ├── CancelBudgetIntegrationListener.java   # remover catch que engole exceção (alterar)
└── resources/db/migration/
    └── V06__create_inbox_messages_table.sql       # nova migration
```

As regras de negócio existentes (`CreateWorkOrderBudgetUseCase`, `CalculateWorkOrderBudgetUseCase`,
`CancelWorkOrderBudgetUseCase`) **não são alteradas**. A deduplicação é uma preocupação transversal aplicada antes
do despacho para o listener de domínio.

## 4. Contrato de dados

### 4.1 Migration `V06__create_inbox_messages_table.sql`

```sql
CREATE TABLE inbox_messages
(
    message_id   UUID         NOT NULL,
    consumer     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    aggregate_id UUID,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    received_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    last_error   TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_inbox_messages PRIMARY KEY (message_id, consumer),
    CONSTRAINT ck_inbox_status CHECK (status IN ('PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_inbox_processed_at
    ON inbox_messages (processed_at)
    WHERE status = 'PROCESSED';

CREATE INDEX idx_inbox_aggregate
    ON inbox_messages (aggregate_id, received_at);
```

A coluna `status = 'FAILED'` fica prevista para evolução futura (auditoria de falhas persistentes), mas no fluxo
transacional ela não é gravada: uma falha gera rollback e a linha desaparece.

### 4.2 Modelo de domínio

```java
public record InboxEntry(
        UUID messageId,
        String consumer,
        String eventType,
        UUID aggregateId,
        Instant receivedAt
) {
}
```

### 4.3 Porta

```java
public interface InboxGateway {

    /**
     * Tenta reservar a mensagem para processamento.
     * @return true se a mensagem ainda não foi processada; false se for duplicada.
     */
    boolean tryStart(InboxEntry entry);

    void markProcessed(InboxEntry entry);

    int deleteProcessedBefore(Instant cutoff);
}
```

### 4.4 Caso de uso transacional

```java
public class ProcessInboundMessageUseCase {

    private final InboxGateway inboxGateway;

    public ProcessInboundMessageUseCase(InboxGateway inboxGateway) {
        this.inboxGateway = inboxGateway;
    }

    @Transactional
    public boolean execute(InboxEntry entry, Runnable handler) {
        if (!inboxGateway.tryStart(entry)) {
            return false;
        }

        handler.run();
        inboxGateway.markProcessed(entry);
        return true;
    }
}
```

O bean é registrado em `InboxConfiguration`:

```java
@Bean
public ProcessInboundMessageUseCase processInboundMessageUseCase(InboxGateway inboxGateway) {
    return new ProcessInboundMessageUseCase(inboxGateway);
}
```

### 4.5 Adaptador JDBC

```java
private static final String TRY_START_SQL = """
        INSERT INTO inbox_messages
            (message_id, consumer, event_type, aggregate_id, status, received_at, created_at, updated_at)
        VALUES
            (:messageId, :consumer, :eventType, :aggregateId, 'PROCESSING', now(), now(), now())
        ON CONFLICT (message_id, consumer) DO NOTHING
        """;

private static final String MARK_PROCESSED_SQL = """
        UPDATE inbox_messages
        SET status = 'PROCESSED',
            processed_at = now(),
            updated_at = now()
        WHERE message_id = :messageId
          AND consumer = :consumer
        """;
```

`tryStart` retorna `jdbcTemplate.update(...) == 1`.

## 5. Resolução do `messageId`

No `BudgetQueueListener`, a ordem de resolução deve ser:

1. **`MessageProperties.messageId`** — preenchido pelo `RabbitEventSender` com o `id` da outbox. É a fonte
   preferencial e independe do payload.
2. **Hash determinístico** de `eventType + ":" + corpo da mensagem` — para produtores que não enviam `messageId`.
   Garante que reentregas da mesma mensagem física gerem a mesma chave.
3. Se o valor do header não for um UUID válido, normalizar com `UUID.nameUUIDFromBytes(valor.getBytes(UTF_8))`.
4. Registrar `WARN` quando o fallback for usado, para cobrar a correção no produtor.

```java
private UUID resolveMessageId(Message message, String eventType) {
    String headerId = message.getMessageProperties().getMessageId();

    if (headerId != null && !headerId.isBlank()) {
        try {
            return UUID.fromString(headerId);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(headerId.getBytes(StandardCharsets.UTF_8));
        }
    }

    byte[] body = message.getBody();
    byte[] key = (eventType + ":").getBytes(StandardCharsets.UTF_8);
    byte[] combined = new byte[key.length + body.length];
    System.arraycopy(key, 0, combined, 0, key.length);
    System.arraycopy(body, 0, combined, key.length, body.length);

    logger.warn("Mensagem sem messageId na fila {} (eventType={}); usando chave determinística",
            consumer, eventType);
    return UUID.nameUUIDFromBytes(combined);
}
```

> O fallback por hash pode deduplicar comandos legítimos que tenham payload idêntico. Por isso o caminho ideal é
> todos os produtores enviarem `messageId`. O `RabbitEventSender` já faz isso.

## 6. Fluxo do listener

O `BudgetQueueListener` passa a delegar o despacho para o caso de uso transacional:

```java
@RabbitListener(queues = "${app.rabbitmq.queues.budget}")
public void onMessage(Message message) {
    String eventType = resolveEventType(message);
    if (eventType == null) {
        logger.warn("Mensagem sem header de eventType; ignorada");
        return;
    }

    UUID messageId = resolveMessageId(message, eventType);
    InboxEntry entry = new InboxEntry(messageId, consumer, eventType, null, Instant.now());

    boolean processed = processInboundMessageUseCase.execute(entry, () -> dispatch(message, eventType));

    if (processed) {
        meterRegistry.counter("inbox.messages.processed").increment();
    } else {
        meterRegistry.counter("inbox.messages.duplicated").increment();
        logger.info("Mensagem duplicada ignorada (messageId={}, eventType={}, consumer={})",
                messageId, eventType, consumer);
    }
}
```

`dispatch` mantém o roteamento atual por `event-type`. A exceção **não** pode ser capturada dentro do `Runnable`;
ela deve propagar até o proxy transacional para provocar o rollback.

### 6.1 Alteração obrigatória nos listeners de integração

Remover o `try/catch` de `GenerateBudgetIntegrationListener` e `CancelBudgetIntegrationListener`. O log de erro e a decisão de retry passam a ser responsabilidade do container
(não da regra de negócio).

```java
// antes
try {
    useCase.execute(command);
} catch (Exception e) {
    logger.error("Erro ao processar ...", e);
}

// depois
useCase.execute(command);
```

## 7. Tratamento de falhas: retry e DLQ

A inbox só entrega o benefício completo se a mensagem não for descartada em caso de falha. O comportamento atual
(`default-requeue-rejected: false` + exceção engolida) descarta a mensagem.

### 7.1 Configuração de retry

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: auto
        default-requeue-rejected: false
        retry:
          enabled: true
          max-attempts: 5
          initial-interval: 1s
          multiplier: 2
          max-interval: 30s
```

### 7.2 Dead Letter Queue

Declarar em `RabbitConfig`:

- exchange `finance-dlx` (direct, durável);
- fila `budget.dlq` (durável);
- binding `budget.dlq` ← `finance-dlx` com routing key `budget`;
- argumentos na `budgetQueue`:

```java
QueueBuilder.durable(queueName)
        .deadLetterExchange(deadLetterExchange)
        .deadLetterRoutingKey(queueName)
        .build();
```

Fluxo:

```text
budget ──falha 1..5──> retry com backoff ──esgotou──> budget.dlq
```

A DLQ deve ser monitorada. O reprocessamento é manual e deve respeitar a inbox: reenviar a mensagem para `budget`
fará com que mensagens já processadas sejam ignoradas e apenas as que falharam sejam executadas.

> **Atenção na migração:** o RabbitMQ não permite alterar os argumentos de uma fila existente. Como a fila `budget`
> passa a declarar `x-dead-letter-exchange` e `x-dead-letter-routing-key`, ela precisa ser drenada e recriada no
> deploy (ou receber a DLX por policy, sem alterar a declaração). O `RabbitAdmin` falha ao redeclarar uma fila com
> argumentos diferentes dos existentes.

## 8. Observabilidade

### 8.1 Métricas

| Métrica | Tipo | Descrição |
|---------|------|-----------|
| `inbox.messages.processed` | Counter | Mensagens processadas com sucesso |
| `inbox.messages.duplicated` | Counter | Mensagens ignoradas por duplicidade |
| `inbox.messages.failed` | Counter | Falhas que provocaram rollback |
| `inbox.cleanup.deleted` | Counter | Registros removidos pela retenção |

### 8.2 Logs

Incluir `messageId`, `eventType` e `consumer` nos logs de início, duplicidade e falha. Se o projeto adotar MDC, esses
campos devem ser propagados no contexto da mensagem.

### 8.3 Alertas sugeridos

- crescimento de `inbox.messages.duplicated` acima do baseline;
- profundidade de `budget.dlq` maior que zero por mais de 5 minutos;
- `inbox.messages.failed` com taxa crescente.

## 9. Plano de ação

### Fase 0 — Alinhamento e contrato (0,5 dia)

- [ ] Aceitar este ADR.
- [ ] Confirmar que todos os produtores enviam `messageId` no AMQP (o `RabbitEventSender` já envia).
- [ ] Confirmar retenção da inbox (30 dias) e política de retry (5 tentativas + DLQ).
- [ ] Confirmar que os consumidores atuais só produzem efeitos transacionais (banco + outbox), sem chamadas HTTP.

### Fase 1 — Core (1 dia)

- [ ] Criar `InboxEntry`.
- [ ] Criar `InboxGateway`.
- [ ] Criar `ProcessInboundMessageUseCase` com `@Transactional`.
- [ ] Registrar o bean em `InboxConfiguration`.
- [ ] Testes unitários:
  - primeira entrega executa o handler e marca como processada;
  - duplicata não executa o handler;
  - exceção no handler propaga e não marca como processada.

### Fase 2 — Persistência (1 dia)

- [ ] Criar `V06__create_inbox_messages_table.sql`.
- [ ] Criar `JdbcInboxGateway` com `ON CONFLICT DO NOTHING`.
- [ ] Criar `InboxProperties` (`app.inbox.*`).
- [ ] Criar `InboxCleanupJob` com `@Scheduled`.
- [ ] Criar `InboxConfiguration` com `@EnableConfigurationProperties` e métricas.
- [ ] Adicionar configuração em `application.yml` e `application-local.yml`.
- [ ] Testes de integração com Testcontainers:
  - inserção + duplicidade no mesmo consumer;
  - mesma mensagem em consumers diferentes é processada;
  - limpeza remove apenas `PROCESSED` antigas;
  - concorrência: duas threads, apenas uma vence.

### Fase 3 — Integração com a mensageria (1 dia)

- [ ] Alterar `BudgetQueueListener` para resolver `messageId` e chamar o caso de uso transacional.
- [ ] Remover `try/catch` dos três listeners de integração.
- [ ] Incrementar métricas de processado/duplicado/erro.
- [ ] Ajustar `BudgetQueueListenerTest`.
- [ ] Adicionar testes cobrindo:
  - `messageId` vindo do AMQP;
  - fallback determinístico;
  - duplicidade não despacha para o listener;
  - exceção do listener propaga.

### Fase 4 — Retry e DLQ (1 dia)

- [ ] Declarar `finance-dlx` e `budget.dlq` em `RabbitConfig`.
- [ ] Adicionar argumentos de dead-letter na `budgetQueue`.
- [ ] Habilitar retry no `application.yml`.
- [ ] Atualizar `RabbitConfigTest`.
- [ ] Validar manualmente com RabbitMQ:
  - falha temporária → retry com backoff;
  - falha permanente → DLQ.

### Fase 5 — Observabilidade e operação (0,5 dia)

- [ ] Publicar métricas no Prometheus.
- [ ] Criar alertas e painel.
- [ ] Documentar procedimento de replay da DLQ no ADR ou runbook.
- [ ] Validar que os logs permitem rastrear `messageId` de ponta a ponta.

### Fase 6 — Rollout (0,5 dia)

- [ ] Deploy da migration (Flyway) junto com o código.
- [ ] Monitorar duplicidades, falhas e DLQ nas primeiras 24h.
- [ ] Rollback: desligar a inbox por propriedade (`app.inbox.enabled=false`) ou reverter o wiring do listener.
      A tabela pode permanecer sem impacto.

**Estimativa total:** ~5 dias úteis.

## 10. Testes de aceite

1. A mesma mensagem entregue duas vezes executa a regra de negócio **uma única vez**; a segunda é confirmada
   (`ack`) e contabilizada como duplicada.
2. Falha na regra de negócio provoca rollback; o registro da inbox não permanece e a mensagem é reentregue.
3. Commit do banco bem-sucedido seguido de falha de `ack` não gera reprocessamento na reentrega.
4. Duas instâncias consumindo a mesma mensagem simultaneamente processam apenas uma vez.
5. Mensagem sem `messageId` usa chave determinística e continua idempotente para reentregas idênticas.
6. Após esgotar as tentativas, a mensagem vai para `budget.dlq` e não fica em loop.
7. Registros `PROCESSED` antigos são removidos pela rotina de retenção.
8. Os casos de uso de domínio permanecem inalterados.

## 11. Riscos e trade-offs

| Risco | Mitigação |
|-------|-----------|
| Efeitos colaterais não transacionais (e-mail, HTTP) não são cobertos pela inbox | Não há consumidor com esse perfil hoje; se surgir, usar chave de idempotência no próprio serviço externo |
| Fallback por hash deduplicar comandos legítimos idênticos | Priorizar `messageId` no produtor e monitorar logs de fallback |
| Mudança no tratamento de exceção expõe falhas antes silenciosas | Retry + DLQ implementados na Fase 4 antes do rollout |
| Crescimento da tabela | Retenção de 30 dias + índice em `processed_at` |
| Replay de DLQ após a retenção | Retenção deve ser maior que a janela operacional de reprocessamento |
| Ordem das mensagens não é garantida | A inbox garante idempotência, não ordenação; se necessário, evoluir para controle de versão por agregado |
| Mensagens com `messageId` não UUID | Normalização determinística via `UUID.nameUUIDFromBytes` |

## 12. Consequências

- O serviço passa a ter **outbox + inbox**: publicação confiável e consumo idempotente.
- O efeito no banco de dados torna-se *effectively-once* para mensagens entregues ao menos uma vez.
- A regra de negócio deixa de se preocupar com duplicidade.
- O tratamento de erros passa a ser explícito: falha = rollback + retry + DLQ, e não mais log + descarte.
- A tabela `inbox_messages` passa a ser um artefato operacional que exige monitoramento e limpeza.
