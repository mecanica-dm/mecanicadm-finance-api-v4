# [006]: [Rota pública de decisão do orçamento por link com token]

* **Status**: Aceito
* **Data**: 12/11/2026

## 1. Contexto e Problema

O orçamento é enviado ao cliente por e-mail, e é o **cliente** quem aprova, rejeita ou pede alterações. O cliente não tem
conta nem JWT. A API, porém, exige JWT em tudo, e a rota que o link do e-mail abre estava **bloqueada**: o cliente que
clicasse receberia 401. O problema passava despercebido porque o perfil `ci` libera todas as rotas.

## 2. Decisão

### 2.1 Um link com token de uso único

O e-mail leva três links (aprovar, rejeitar, pedir alterações), todos com um token próprio do orçamento.

| Característica | Valor |
|---|---|
| Rota | `GET /budget-decision/{token}/form` (formulário) e `POST /budget-decision/{token}` (decisão) |
| Token | UUID aleatório, único por envio, guardado em `budget_decision_tokens` |
| Validade | **24 horas** a partir da criação |
| Uso | **Único**: depois de decidir, o token é marcado como usado |
| Token inexistente, usado ou expirado | Página de erro com a explicação, sem executar nada |

### 2.2 Somente essa rota é pública

`/budget-decision/**` passa a ser liberada no `SecurityConfig`, junto com o Swagger, o `/error` e o Actuator. **Todo o resto
exige JWT**, inclusive enviar, recalcular e decidir manualmente o orçamento pelo painel.

### 2.3 Teste de integração

O `PublicRoutesSecurityIT` roda com o perfil de teste (a cadeia de segurança real) e confere que o link abre sem JWT e que as
rotas internas de orçamento e pagamento continuam respondendo 401 sem JWT.

## 3. Consequências

### Positivas

- O cliente decide pelo link do e-mail, sem criar conta, e o fluxo de aprovação funciona de ponta a ponta.
- A superfície pública é mínima, e o token de uso único limita o dano de um link vazado.

### Negativas / Custos

- **Quem tem o link decide pelo cliente** durante 24 horas, se ele não tiver sido usado. O sigilo do e-mail é a proteção.
- Não há limitação de tentativas na rota pública, nem verificação de quem abre o link.
- Depois de 24 horas, o link deixa de valer. Uma rotina avisa a saga uma única vez (`BudgetExpiredEvent`,
  [RFC 001](../rfc/001-expiracao-do-orcamento.md)), e ela cancela a OS; até a rotina rodar, há um atraso de até o intervalo
  configurado.

## 4. Relação com Outras Decisões

- [RFC 001 - Expiração do orçamento](../rfc/001-expiracao-do-orcamento.md)
- ADR 006 da Saga API (JWT compartilhado): o resto das rotas do sistema usa o mesmo token.
