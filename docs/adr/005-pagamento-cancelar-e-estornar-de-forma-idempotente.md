# [005]: [Pagamento: gerar, cancelar e estornar de forma idempotente]

* **Status**: Aceito
* **Data**: 12/11/2026

## 1. Contexto e Problema

A saga precisa de três operações de pagamento: **gerar** o pedido quando a execução termina, **cancelar** o pedido ainda
não pago e **estornar** um pagamento confirmado, quando a OS é cancelada. Todas chegam por mensagem, que pode ser entregue
mais de uma vez, e o Mercado Pago responde com erro a repetição de algumas dessas chamadas (por exemplo, cancelar um pedido
já cancelado).

A saga também pode pedir o cancelamento **enquanto o link ainda está sendo gerado**, ou seja, sem pagamento algum registrado.

## 2. Decisão

### 2.1 Gerar (`GeneratePaymentIntegrationCommand`)

- O valor, a descrição e o pagador vêm do **orçamento da OS** (total, descrição, nome e e-mail do cliente). Sem
  orçamento, o command é recusado.
- A **chave de idempotência e a referência externa são o `workOrderId`**: há no máximo um pedido por OS, e repetir o command
  devolve o pedido já criado, sem criar outro.
- O cartão vem da configuração (`MERCADO_PAGO_DEFAULT_CARD_*`): é o cartão de teste do Mercado Pago, e o titular `APRO`
  aprova o pagamento no ambiente de testes. Isso torna a demonstração possível sem uma tela de cartão.
- O resultado imediato vira `PaymentLinkGeneratedEvent` e, se o pedido já vier aprovado, `PaymentConfirmedEvent`. Se falhar,
  `PaymentLinkGenerationFailedEvent`.

### 2.2 Cancelar (`CancelPaymentIntegrationCommand`)

| Situação do pagamento | Reação |
|---|---|
| Não existe (link ainda não gerado) | Só confirma o cancelamento (`PaymentCanceledEvent`) |
| Já cancelado | Confirma de novo, sem chamar o Mercado Pago |
| Pendente | Cancela o pedido no Mercado Pago, marca `canceled` e confirma |
| Mercado Pago responde 409 `order_already_canceled` | Trata como feito e confirma |
| Já confirmado ou estornado | **Recusa** (`PaymentAlreadyConfirmed`): pagamento confirmado só se estorna |

### 2.3 Estornar (`RefundPaymentIntegrationCommand`)

| Situação do pagamento | Reação |
|---|---|
| Não existe | **Recusa** (`PaymentNotFound`) |
| Já estornado | Confirma de novo (`PaymentRefundedEvent`), sem chamar o Mercado Pago |
| Confirmado | Estorna o pedido (total, sem informar valor), marca `refunded` e confirma |
| Qualquer outro | **Recusa** (`PaymentNotRefundable`) |

### 2.4 Regras comuns

- As chamadas ao Mercado Pago levam `X-Idempotency-Key` própria: `cancel-<id do pagamento>` e `refund-<id do pagamento>`.
  Repetir a chamada depois de uma falha parcial não cancela nem estorna duas vezes.
- O **status do pagamento é o nosso** (`canceled`, `refunded`), e não o do Mercado Pago: depois de um estorno total, o
  pedido continua com `status: processed` e muda só o `status_detail` para `refunded`.
- Cancelar e estornar **sempre publicam a confirmação** quando já estão feitos, porque a saga só encerra o cancelamento
  quando a recebe.
- O token de acesso vem do `MercadoPagoAccessTokenProvider`: o access token configurado ou, na falta dele, as credenciais
  do aplicativo.

## 3. Consequências

### Positivas

- Repetir qualquer um dos três commands é seguro, e a saga pode reenviar sem medo.
- O cancelamento durante a geração do link não trava a saga.
- Uma recusa (estorno de pagamento inexistente, cancelamento de pagamento já pago) chega à saga como evento, e ela decide.

### Negativas / Custos

- **Estorno com corpo vazio não validado no sandbox.** A documentação do Mercado Pago pede, no guia, o corpo vazio para o
  estorno total, e a referência da API lista `transactions` como obrigatório. Seguimos o guia e testamos com um servidor
  simulado; falta confirmar contra o ambiente de testes real.
- Um pedido que fique pendente e seja pago logo antes de um cancelamento ainda pode produzir um 409 `cannot_cancel_order`
  do Mercado Pago, que chega à saga como recusa. A [RFC 002](../rfc/002-verificacao-de-pagamento.md) reduz a janela: a rotina
  de consulta publica o resultado, de modo que o Finance fica sabendo do pagamento.
- O cartão de teste fixo serve para a demonstração, não para produção.

## 4. Relação com Outras Decisões

- [RFC 002 - Verificação de pagamento](../rfc/002-verificacao-de-pagamento.md)
- ADR 002 da Saga API (compensação com confirmação e pausa): é ela que consome os eventos de cancelamento e estorno.
- [ADR 004 - Evento de recusa de command](004-evento-de-recusa-de-command.md)
