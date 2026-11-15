# language: pt
Funcionalidade: Fluxo completo do Finance, do orçamento ao pagamento
  Como oficina, quero que o Finance gere o orçamento da OS, colha a decisão do cliente pelo e-mail
  e cobre o serviço pelo Mercado Pago, avisando a saga a cada passo.

  Cenário: O orçamento é aprovado pelo cliente e o pagamento é confirmado
    Dado que o Finance recebeu o pedido de orçamento de uma OS com mão de obra de 80.00 e material de 2 x 50.00
    Então o orçamento da OS está "PENDING" com total de 180.00
    E o Finance publicou o evento "BudgetGeneratedEvent"

    Quando o orçamento é enviado ao cliente
    Então o orçamento da OS está "WAITING_DECISION" com total de 180.00
    E o cliente recebeu um e-mail com os links de decisão do orçamento
    E o Finance publicou o evento "BudgetSentForDecisionEvent"

    Quando o cliente aprova o orçamento pelo link do e-mail
    Então o orçamento da OS está "APPROVED" com total de 180.00
    E o link de decisão fica marcado como usado
    E o Finance publicou o evento "BudgetApprovedEvent"

    Quando a saga pede o link de pagamento
    Então o pagamento da OS está "processed"
    E o link de pagamento foi informado à saga
    E os eventos publicados, em ordem, são "BudgetGeneratedEvent, BudgetSentForDecisionEvent, BudgetApprovedEvent, PaymentLinkGeneratedEvent, PaymentConfirmedEvent"

  Cenário: O pagamento fica pendente e a verificação periódica confirma
    Dado que o Finance recebeu o pedido de orçamento de uma OS com mão de obra de 80.00 e material de 2 x 50.00
    E que o cliente aprovou o orçamento
    E que o Mercado Pago deixa o pagamento pendente
    Quando a saga pede o link de pagamento
    Então o pagamento da OS está "action_required"
    E o Finance publicou o evento "PaymentLinkGeneratedEvent"
    E o Finance não publicou o evento "PaymentConfirmedEvent"

    Quando a verificação periódica consulta os pagamentos pendentes
    Então o Finance não publicou o evento "PaymentConfirmedEvent"

    Quando o Mercado Pago passa o pagamento para "processed"
    E a verificação periódica consulta os pagamentos pendentes
    Então o pagamento da OS está "processed"
    E o Finance publicou o evento "PaymentConfirmedEvent"

  Cenário: Cancelar a OS depois do pagamento estorna o valor e cancela o orçamento
    Dado que o Finance recebeu o pedido de orçamento de uma OS com mão de obra de 80.00 e material de 2 x 50.00
    E que o cliente aprovou o orçamento
    E que o pagamento da OS foi gerado
    Quando a saga manda estornar o pagamento
    Então o pagamento da OS está "refunded"
    E o Finance publicou o evento "PaymentRefundedEvent"

    Quando a saga manda cancelar o orçamento
    Então o orçamento da OS está "CANCELED"
    E o Finance publicou o evento "BudgetCanceledEvent"
