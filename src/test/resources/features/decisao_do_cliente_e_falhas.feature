# language: pt
Funcionalidade: Decisão do cliente e falhas no Finance
  Como oficina, quero que o Finance trate a rejeição e o pedido de alterações do cliente, recuse
  ações em orçamentos que já não aceitam mudança e avise a saga quando o pagamento falha.

  Contexto:
    Dado que o Finance recebeu o pedido de orçamento de uma OS com mão de obra de 80.00 e material de 2 x 50.00

  Cenário: O cliente rejeita o orçamento
    Dado que o orçamento já foi enviado ao cliente
    Quando o cliente rejeita o orçamento pelo link do e-mail com o motivo "Muito caro"
    Então o orçamento da OS está "REJECTED"
    E o Finance publicou o evento "BudgetRejectedEvent"
    E o link de decisão fica marcado como usado

  Cenário: O cliente pede alterações e recebe um novo orçamento
    Dado que o orçamento já foi enviado ao cliente
    Quando o cliente pede alterações pelo link do e-mail com a observação "Trocar só o óleo"
    Então o orçamento da OS está "CHANGES_REQUESTED"
    E o Finance publicou o evento "BudgetChangesRequestedEvent"

    Quando o orçamento é recalculado para 150.00
    Então o orçamento da OS está "PENDING" com total de 150.00

    Quando o orçamento é enviado ao cliente
    E o cliente aprova o orçamento pelo link do e-mail
    Então o orçamento da OS está "APPROVED" com total de 150.00
    E o Finance publicou o evento "BudgetApprovedEvent"

  Cenário: O link de decisão só vale uma vez
    Dado que o orçamento já foi enviado ao cliente
    Quando o cliente aprova o orçamento pelo link do e-mail
    E o cliente tenta usar o mesmo link de novo
    Então a operação é recusada com "budget.token.invalid"
    E o orçamento da OS está "APPROVED"

  Cenário: Orçamento aprovado não pode ser recalculado
    Dado que o cliente aprovou o orçamento
    Quando o orçamento é recalculado para 999.00
    Então a operação é recusada com "budget.not.editable"
    E o orçamento da OS está "APPROVED" com total de 180.00

  Cenário: Orçamento cancelado não aceita recálculo nem novo envio
    Dado que o orçamento já foi enviado ao cliente
    Quando a saga manda cancelar o orçamento
    Então o orçamento da OS está "CANCELED"

    Quando o orçamento é recalculado para 999.00
    Então a operação é recusada com "budget.not.editable"
    E o orçamento da OS está "CANCELED" com total de 180.00

  Cenário: O Mercado Pago recusa o pagamento e a saga é avisada
    Dado que o cliente aprovou o orçamento
    E que o Mercado Pago recusa o pagamento
    Quando a saga pede o link de pagamento
    Então o pagamento da OS está "rejected"
    E o Finance publicou o evento "PaymentFailedEvent"
    E o Finance não publicou o evento "PaymentConfirmedEvent"

  Cenário: Falha ao gerar o pagamento avisa a saga para pausar
    Dado que o cliente aprovou o orçamento
    E que o Mercado Pago está indisponível para a conta
    Quando a saga pede o link de pagamento e o Mercado Pago falha
    Então o Finance publicou o evento "PaymentLinkGenerationFailedEvent"
    E o Finance não publicou o evento "PaymentLinkGeneratedEvent"

  Cenário: Cancelar um pagamento ainda pendente
    Dado que o cliente aprovou o orçamento
    E que o Mercado Pago deixa o pagamento pendente
    E que o pagamento da OS foi gerado
    Quando a saga manda cancelar o pagamento
    Então o pagamento da OS está "canceled"
    E o Finance publicou o evento "PaymentCanceledEvent"
