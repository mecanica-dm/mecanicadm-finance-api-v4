# [004]: [Evento de recusa de command]

* **Status**: Aceito
* **Data**: 12/11/2026

## 1. Contexto e Problema

A saga envia commands ao Finance (gerar e cancelar orçamento; gerar, cancelar e estornar pagamento) e espera um evento de
resposta. Quando o Finance **não consegue executar** o command (por exemplo, cancelar o orçamento de uma OS que não tem
orçamento, ou gerar o pagamento sem orçamento), a mensagem ia para a DLQ e **ninguém era avisado**. A saga ficava esperando
uma resposta que não vinha, e o fluxo travava em silêncio.

## 2. Decisão

Quando o listener de um command falha com uma **exceção de domínio** (`DomainExceptionCore`), o Finance publica o
`FinanceCommandRejectedEvent`:

| Campo | Conteúdo |
|---|---|
| `workOrderId` | A OS do command |
| `eventType` | O nome do command (por exemplo, `GenerateBudgetIntegrationCommand`) |
| `exception` | O nome simples da exceção (por exemplo, `BudgetNotFound`) |
| `message` | A mensagem da exceção |

- O evento é publicado pelo `RejeitarFinanceCommandUseCase`, em uma **transação própria** (`REQUIRES_NEW`), para sair mesmo
  que a transação do command seja desfeita.
- **Só exceção de domínio gera o evento.** Falha técnica (banco fora, erro inesperado) não avisa a saga: segue o caminho
  normal de falha, com as retentativas e a DLQ. É a mesma política da Workorder (ADR 012 da Workorder API).
- O nome da classe é `FinanceCommandRejectedEvent`, com o sufixo `Event` como todos os outros eventos do Finance. O header
  `event-type` segue o nome da classe, então o nome precisa ser exatamente o que a saga espera.
- Depois de publicar, o listener **relança a exceção**: a transação do inbox é desfeita e a mensagem segue o caminho de
  falha do RabbitMQ (retentativas com espera crescente e, por fim, a DLQ).
- **Uma recusa por mensagem, e não por tentativa.** O id do evento é **derivado do id da mensagem recebida** (e do nome do
  command), guardado na thread do listener (`InboundMessageContext`). As tentativas do retry reprocessam a mesma mensagem,
  geram o mesmo id, e a outbox ignora o duplicado (`ON CONFLICT DO NOTHING`).
- A saga só reage se o command recusado for o que o estado dela espera (ADR 005 da Saga API).

## 3. Consequências

### Positivas

- A saga deixa de ficar presa e pode compensar ou pausar com o motivo registrado.
- O motivo fica no histórico da saga e no log.
- Uma falha transitória não faz a saga cancelar uma OS.
- Cada mensagem recusada gera um único evento, mesmo com as 5 tentativas do retry.
- A mensagem continua indo para a DLQ, onde pode ser analisada e reprocessada.

### Negativas / Custos

- **Algumas exceções de domínio do Finance têm causa externa.** `MercadoPagoProviderError` estende `DomainExceptionCore`, então
  uma falha do Mercado Pago ao gerar o pagamento também gera a recusa (além do `PaymentLinkGenerationFailedEvent`). Na
  prática a saga já pausou pelo evento de falha do link e ignora a recusa seguinte, porque o estado deixou de esperar o
  command.
- **Reprocessar a mesma mensagem da DLQ não gera um segundo evento**, pois o id é o mesmo e a outbox o descarta. A saga já
  recebeu a primeira recusa; se ela precisar de nova resposta, o command deve ser reenviado como uma mensagem nova.
- O `eventType` é o nome da classe do command: renomear um command exige atualizar a saga.

## 4. Relação com Outras Decisões

- ADR 005 da Saga API (eventos de recusa) e ADR 012 da Workorder API (o mesmo evento na Workorder).
- [ADR 003 - Estratégia de inbox](003-estrategia-inbox.md): a transação do inbox é desfeita quando o listener relança.
