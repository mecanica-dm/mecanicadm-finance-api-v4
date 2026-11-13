# RFC 002 - Verificação do Pagamento

* **Status**: Aprovado (implementado em 12/11/2026: rotina de consulta e webhook)
* **Data**: 12/11/2026
* **Autores**: Time de desenvolvimento
* **Resumo**: Define como o Finance **sabe o resultado de um pagamento depois da criação do pedido**, que antes era
  conhecido apenas no instante em que o pedido era criado.

---

## 1. Resumo

O desafio pede "registro e verificação de pagamentos". O Finance cria o pedido no Mercado Pago com o cartão de teste e
publicava `PaymentConfirmedEvent` ou `PaymentFailedEvent` com o resultado **imediato** daquela resposta. Se o pedido ficasse
pendente e fosse aprovado (ou recusado) depois, o Finance não percebia, e a saga ficava em `AWAITING_PAYMENT`.

## 2. Contexto (antes da verificação)

- O pedido é criado com captura automática e cartão de teste; com o titular `APRO`, o Mercado Pago aprova na hora, e a
  demonstração funciona.
- Meios de pagamento assíncronos (Pix, boleto) e análises de risco devolvem o pedido pendente, e o resultado vem depois.
- O Finance guardava a resposta do Mercado Pago em `payments.provider_response`, mas **não consultava de novo**.

## 3. Objetivos

- Detectar a aprovação, recusa ou expiração de um pedido pendente e publicar o evento correspondente.
- Não publicar o mesmo resultado duas vezes.
- Não exigir, no primeiro passo, que a API do Finance seja exposta na internet.

## 4. Alternativas Consideradas

### 4.1. Webhook do Mercado Pago

O Mercado Pago chama uma rota pública do Finance a cada mudança do pedido.

**Prós:** resultado quase em tempo real; é o caminho recomendado pelo Mercado Pago.

**Contras:** exige uma URL pública e a validação da assinatura da notificação; precisa de rota sem JWT (mais uma exceção de
segurança); difícil de reproduzir em ambiente local.

### 4.2. Rotina agendada que consulta os pedidos pendentes

Uma tarefa periódica lista os pagamentos em estado intermediário, consulta `GET /v1/orders/{id}` e publica o resultado quando
mudar.

**Prós:** funciona sem expor a API; fácil de testar; também serve de **reconciliação** quando uma notificação se perde.

**Contras:** latência do intervalo; consome chamadas à API do Mercado Pago.

### 4.3. Manter como está e documentar a limitação

**Prós:** nada a implementar.

**Contras:** não atende ao requisito de verificação; um pagamento pendente trava a saga.

## 5. Solução adotada

Implementada a **alternativa 4.2** (rotina de consulta), que funciona sem expor a API e serve de reconciliação, e o **webhook
(4.1)** para a resposta rápida, quando houver URL pública ([ADR 007](../adr/007-webhook-do-mercado-pago.md)). Os dois usam o mesmo
caso de uso:

| Aspecto | Solução |
|---|---|
| Rotina | `VerifyPendingPaymentsJob`, a cada minuto por padrão (`PAYMENT_VERIFICATION_INTERVAL_MS`), com o caso de uso `VerifyPendingPaymentsUseCase` |
| Seleção | Pagamentos com pedido no Mercado Pago, em estado não final (`PaymentStatuses.FINAL`) e sem verificação há `PAYMENT_VERIFICATION_MIN_AGE_SECONDS` (60), em lotes com `SKIP LOCKED` |
| Consulta | `GET /v1/orders/{id}` com o access token do `MercadoPagoAccessTokenProvider` |
| Efeito | Atualiza o status local e publica `PaymentConfirmedEvent` ou `PaymentFailedEvent`, uma única vez, pela outbox. Se nada mudou, só renova a data da verificação |
| Idempotência | A mudança de estado local e o evento saem na mesma transação; estado final não é consultado de novo |
| Cancelamento | Pagamento cancelado ou estornado pela própria saga não é consultado |
| Webhook | `POST /v1/payments/webhooks/mercado-pago`, público e protegido pela assinatura (`x-signature`); usa só o `data.id`, consulta o pedido no Mercado Pago e aplica a mesma regra, com `FOR UPDATE` para não competir com a rotina |

## 6. Impacto e Riscos

- **Risco**: consultar demais e esbarrar no limite de chamadas do Mercado Pago.
    - **Mitigação**: intervalo e tamanho de lote configuráveis; só pedidos pendentes.
- **Risco**: pagamento confirmado depois da OS cancelada.
    - **Mitigação**: a saga já estorna o pagamento que chega depois do cancelamento (ADR 002 da Saga API).

## 7. Implementação

1. `getOrder` na porta do Mercado Pago e na implementação (`GET /v1/orders/{id}`). (feito)
2. `VerifyPendingPaymentsJob`, `VerifyPendingPaymentsUseCase` e a consulta dos pendentes com `SKIP LOCKED` (`PaymentGateway.findPendingForVerification`). (feito)
3. Publicar os eventos pela outbox, na mesma transação da atualização, reaproveitando o `PaymentOrderEventService`. (feito)
4. Testes: `VerifyPendingPaymentsUseCaseTest` (confirmado, recusado, sem mudança, falha do provedor, sem pendentes e o caminho do webhook), `VerifyPendingPaymentsJobTest`, `PendingPaymentsIT` e o `getOrder` em `MercadoPagoGatewaysTest`. (feito)
5. Webhook: `MercadoPagoWebhookController`, `MercadoPagoWebhookSignatureValidator` e `VerifyPendingPaymentsUseCase.verifyByOrderId`, com os testes `MercadoPagoWebhookControllerTest`, `MercadoPagoWebhookSignatureValidatorTest` e o `PublicRoutesSecurityIT`. (feito)

## 8. Decisão Final

| Item        | Detalhe                                      |
|-------------|----------------------------------------------|
| Verificação | Rotina de consulta e webhook                 |
| Status      | Aprovada e implementada                      |
