# [007]: [Webhook do Mercado Pago: rota pública protegida por assinatura]

* **Status**: Aceito
* **Data**: 12/11/2026

## 1. Contexto e Problema

A rotina de verificação ([RFC 002](../rfc/002-verificacao-de-pagamento.md)) descobre o resultado de um pagamento pendente
com a latência do seu intervalo (1 minuto por padrão) e consome chamadas ao Mercado Pago mesmo quando nada mudou. O Mercado
Pago oferece o **webhook**: ele chama uma URL nossa a cada mudança do pedido, em segundos.

Receber o webhook cria dois problemas:

- **A rota precisa ser pública.** O Mercado Pago não envia JWT, e o resto da API exige.
- **Qualquer um pode chamar uma rota pública.** É preciso saber que a notificação veio mesmo do Mercado Pago, e não confiar
  no que ela diz.

## 2. Decisão

### 2.1 A rota

`POST /v1/payments/webhooks/mercado-pago` (liberada no `SecurityConfig`, só para o `POST`). A URL pública (por exemplo,
`https://<host>/api/v1/payments/webhooks/mercado-pago`) é configurada no painel do Mercado Pago, em **Suas integrações,
Webhooks**, com o evento **Order (Mercado Pago)**. A configuração gera a **assinatura secreta**, que vai para
`MERCADO_PAGO_WEBHOOK_SECRET`.

### 2.2 Autenticidade: a assinatura

O Mercado Pago envia o cabeçalho `x-signature` (`ts=<instante>,v1=<hash>`) e o `x-request-id`. O hash é o HMAC-SHA256, em
hexadecimal, do manifesto `id:<data.id>;request-id:<x-request-id>;ts:<ts>;` com a assinatura secreta como chave, e os pares
ausentes saem do manifesto. A comparação é em tempo constante (`MessageDigest.isEqual`). O `data.id` alfanumérico é testado
em minúsculas e como veio, porque a documentação orienta as minúsculas e os exemplos oficiais divergem.

| Situação | Resposta |
|---|---|
| Segredo não configurado (e assinatura exigida) | `503`, sem processar |
| Assinatura ausente ou inválida | `401`, sem processar |
| Assinatura válida, tipo diferente de `order`, ou sem `data.id` | `200`, ignorada |
| Assinatura válida e pedido de um pagamento nosso | `200`, depois de conferir o pedido |
| Falha ao processar | `500`, para o Mercado Pago repetir a notificação (a rotina de verificação também cobre) |

`PAYMENT_WEBHOOK_SIGNATURE_REQUIRED=false` desliga a validação **só para desenvolvimento**.

### 2.3 O corpo não é a verdade

O Finance **não confia no que a notificação diz**. Ele usa só o `data.id` para localizar o pagamento e **consulta o pedido no
Mercado Pago** (`GET /v1/orders/{id}`, com o nosso token), aplicando a mesma regra da rotina: atualiza o registro e publica
`PaymentConfirmedEvent` ou `PaymentFailedEvent` se o resultado mudou. Uma notificação forjada, no máximo, provoca uma consulta
a mais. Pedido desconhecido ou pagamento já em estado final é ignorado.

### 2.4 Webhook e rotina convivem

- O webhook dá a resposta rápida, e a rotina de verificação fica como **reconciliação**, caso uma notificação se perca, a URL
  esteja fora do ar ou o webhook não esteja configurado.
- Os dois usam o mesmo caso de uso (`VerifyPendingPaymentsUseCase`) e **travam a linha do pagamento** (`FOR UPDATE`; a rotina
  usa `SKIP LOCKED`). Se os dois chegarem juntos, o segundo vê o estado já atualizado e não publica de novo.

## 3. Consequências

### Positivas

- O resultado de um pagamento chega em segundos, sem esperar o intervalo da rotina.
- A rota pública é pequena, validada por assinatura, e nunca age sobre o conteúdo da notificação.
- Sem o segredo, o endpoint recusa tudo (falha fechada), em vez de aceitar notificações sem origem comprovada.

### Negativas / Custos

- **Exige uma URL pública em HTTPS.** Em desenvolvimento, é preciso um túnel (ngrok ou similar) e configurar o painel do
  Mercado Pago a cada nova URL.
- **A validação da assinatura não foi exercitada com uma notificação real do Mercado Pago**, só com testes que seguem a
  documentação. Convém conferir com uma notificação de teste ("Simular notificação" no painel) antes da demonstração.
- Mais uma exceção de segurança pública, ao lado do link de decisão do orçamento ([ADR 006](006-rota-publica-de-decisao-do-orcamento.md)).

## 4. Relação com Outras Decisões

- [RFC 002 - Verificação do pagamento](../rfc/002-verificacao-de-pagamento.md)
- [ADR 005 - Pagamento: gerar, cancelar e estornar de forma idempotente](005-pagamento-cancelar-e-estornar-de-forma-idempotente.md)
