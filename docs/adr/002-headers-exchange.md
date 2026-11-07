# [002]: [Exchange do RabbitMQ do tipo headers]

* **Status**: Aceito
* **Data**: 29/10/2026

## 1. Contexto e Problema

A troca de mensagens da API de finanças utilizava uma exchange do tipo `topic`, com uma routing key por tipo de
evento. O roteamento dependia de strings de routing key duplicadas na configuração, no publisher e no consumer,
o que dificultava a evolução do contrato e não permitia combinar critérios de roteamento.

## 2. Decisão

A exchange `finance-output` passa a ser do tipo `headers`, com um único header de roteamento:

| Item                   | Valor                                                        |
|------------------------|--------------------------------------------------------------|
| Exchange               | `finance-output`                                             |
| Tipo                   | `headers` (durável, não autodeletável)                       |
| Fila                   | `budget`                                                     |
| Header                 | `event-type` (configurável por `RABBITMQ_HEADER_EVENT_TYPE`) |
| Match                  | `x-match = all`                                              |
| Routing key no publish | vazia                                                        |

Os valores de `event-type` são o nome simples da classe do evento, obtido por `getClass().getSimpleName()`.
Não existe enum centralizando os valores: o contrato é a própria classe do evento.

| Evento                             | Valor do header                    |
|------------------------------------|------------------------------------|
| `BudgetApprovedEvent`              | `BudgetApprovedEvent`              |
| `BudgetCanceledEvent`              | `BudgetCanceledEvent`              |
| `BudgetChangesRequestedEvent`      | `BudgetChangesRequestedEvent`      |
| `BudgetExpiredEvent`               | `BudgetExpiredEvent`               |
| `BudgetGeneratedEvent`             | `BudgetGeneratedEvent`             |
| `BudgetRejectedEvent`              | `BudgetRejectedEvent`              |
| `BudgetSentForDecisionEvent`       | `BudgetSentForDecisionEvent`       |
| `BudgetSentForDecisionFailedEvent` | `BudgetSentForDecisionFailedEvent` |
| `BudgetUpdatedEvent`               | `BudgetUpdatedEvent`               |
| `WorkOrderCreatedEvent`            | `WorkOrderCreatedEvent`            |
| `GenerateBudgetIntegrationCommand` | `GenerateBudgetIntegrationCommand` |
| `CancelBudgetIntegrationCommand`   | `CancelBudgetIntegrationCommand`   |

Todo evento implementa `MessageHeader` (`messageId`, `workOrderId`, `ocurredAt`, `eventType`). O método
`eventType()` tem implementação padrão que retorna `getClass().getSimpleName()`, e o publisher publica esse
valor no header configurado. Os bindings e o consumer referenciam as classes diretamente
(`Classe.class.getSimpleName()`).

## 3. Consequências

- A routing key deixa de ser usada; consumidores devem rotear pelo header `event-type`.
- Todos os serviços que publicam ou consomem essa exchange precisam publicar o header e redeclarar seus bindings.
- O RabbitMQ não altera o tipo de uma exchange existente: a virada exige apagar a exchange `topic` antes do deploy.
  Mensagens publicadas sem o header durante a janela de migração não são roteadas e são descartadas.
- Renomear a classe de um evento altera o valor do header e, portanto, é uma breaking change do contrato de mensageria.

## 4. Migração

1. Congelar produtores e consumidores da exchange.
2. Drenar a fila `budget` (mensagens antigas não possuem o header).
3. Apagar a exchange: `rabbitmqctl delete_exchange finance-output`.
4. Subir a nova versão; o `RabbitAdmin` declara a exchange `headers` e os bindings.
5. Validar com `rabbitmqctl list_exchanges name type` e
   `rabbitmqctl list_bindings source_name destination_name arguments`.
6. Atualizar os demais serviços na mesma janela.
