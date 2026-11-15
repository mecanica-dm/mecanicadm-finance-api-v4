package com.mecanicadm.mecanicadm_finance_api.bdd;

import com.mecanicadm.mecanicadm_finance_api.bdd.FinanceTestDoubles.FakeEmailService;
import com.mecanicadm.mecanicadm_finance_api.bdd.FinanceTestDoubles.FakeMercadoPago;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CancelMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.RefundMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.VerifyPendingPaymentsUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CancelMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.RefundMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.VerifyPendingPaymentsCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.LaborExecutionStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderSnapshot;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CalculateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CancelWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CreateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.ProcessBudgetDecisionByTokenUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.SendWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CalculateWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CancelWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CreateWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.ProcessBudgetDecisionByTokenCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.SendWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.shared.exception.DomainExceptionCore;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinanceStepDefinitions {

    private static final String CLIENT_EMAIL = "cliente@teste.com";
    private static final String BASE_URL = "http://finance.test/api";

    @Autowired
    private CreateWorkOrderBudgetUseCase createBudget;
    @Autowired
    private SendWorkOrderBudgetUseCase sendBudget;
    @Autowired
    private ProcessBudgetDecisionByTokenUseCase decideByToken;
    @Autowired
    private CalculateWorkOrderBudgetUseCase calculateBudget;
    @Autowired
    private CancelWorkOrderBudgetUseCase cancelBudget;
    @Autowired
    private CreateMercadoPagoOrderUseCase createPayment;
    @Autowired
    private VerifyPendingPaymentsUseCase verifyPayments;
    @Autowired
    private CancelMercadoPagoPaymentUseCase cancelPayment;
    @Autowired
    private RefundMercadoPagoPaymentUseCase refundPayment;
    @Autowired
    private WorkOrderBudgetGateway budgetGateway;
    @Autowired
    private FakeMercadoPago mercadoPago;
    @Autowired
    private FakeEmailService emailService;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID workOrderId;
    private String lastToken;
    private Throwable lastError;

    @Before
    public void reset() {
        jdbc.update("DELETE FROM outbox_messages");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM budget_decision_tokens");
        jdbc.update("DELETE FROM work_order_budgets");
        mercadoPago.reset();
        emailService.reset();
        workOrderId = UUID.randomUUID();
        lastToken = null;
        lastError = null;
    }

    // ---------- Dado ----------

    @Dado("que o Finance recebeu o pedido de orçamento de uma OS com mão de obra de {bigdecimal} e material de {int} x {bigdecimal}")
    public void orcamentoSolicitado(BigDecimal labor, int quantity, BigDecimal materialPrice) {
        BigDecimal total = labor.add(materialPrice.multiply(BigDecimal.valueOf(quantity)));
        createBudget.execute(new CreateWorkOrderBudgetCommand(
                workOrderId, total, null, null, "Revisao completa",
                new WorkOrderSnapshot.Client(UUID.randomUUID(), "Cliente BDD", CLIENT_EMAIL, "11122233344", "48999999999"),
                new WorkOrderSnapshot.Vehicle("BDD-0001", "Onix LT", "Chevrolet", (short) 2020),
                List.of(new WorkOrderSnapshot.MaterialItem(UUID.randomUUID(), UUID.randomUUID(), quantity, "Oleo 5W30", "Brand X",
                        "Oleo sintetico", materialPrice, "PART")),
                List.of(new WorkOrderSnapshot.LaborItem(UUID.randomUUID(), workOrderId, UUID.randomUUID(), null, null,
                        LaborExecutionStatus.AWAITING_EXECUTION, "Troca de oleo", labor))));
    }

    @Dado("que o orçamento já foi enviado ao cliente")
    public void orcamentoJaEnviado() {
        enviarAoCliente();
    }

    @Dado("que o cliente aprovou o orçamento")
    public void clienteJaAprovou() {
        enviarAoCliente();
        decidir("APPROVED", null);
    }

    @Dado("que o Mercado Pago deixa o pagamento pendente")
    public void mercadoPagoPendente() {
        mercadoPago.createOrdersAs("action_required");
    }

    @Dado("que o Mercado Pago recusa o pagamento")
    public void mercadoPagoRecusa() {
        mercadoPago.createOrdersAs("rejected");
    }

    @Dado("que o Mercado Pago está indisponível para a conta")
    public void mercadoPagoIndisponivel() {
        mercadoPago.failOnCreate();
    }

    @Dado("que o pagamento da OS foi gerado")
    public void pagamentoJaGerado() {
        gerarPagamento();
    }

    // ---------- Quando ----------

    @Quando("o orçamento é enviado ao cliente")
    public void enviarAoCliente() {
        sendBudget.execute(new SendWorkOrderBudgetCommand(workOrderId, BASE_URL));
        lastToken = jdbc.queryForObject(
                "SELECT token FROM budget_decision_tokens WHERE work_order_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class, workOrderId);
    }

    @Quando("o cliente aprova o orçamento pelo link do e-mail")
    public void clienteAprova() {
        decidir("APPROVED", null);
    }

    @Quando("o cliente rejeita o orçamento pelo link do e-mail com o motivo {string}")
    public void clienteRejeita(String reason) {
        decidir("REJECTED", reason);
    }

    @Quando("o cliente pede alterações pelo link do e-mail com a observação {string}")
    public void clientePedeAlteracoes(String observation) {
        decidir("CHANGES_REQUESTED", observation);
    }

    @Quando("o cliente tenta usar o mesmo link de novo")
    public void clienteUsaLinkDeNovo() {
        lastError = capture(() -> decidirComToken("APPROVED", null));
    }

    @Quando("o orçamento é recalculado para {bigdecimal}")
    public void recalcular(BigDecimal total) {
        lastError = capture(() -> calculateBudget.execute(new CalculateWorkOrderBudgetCommand(workOrderId, total)));
    }

    @Quando("a saga pede o link de pagamento")
    public void gerarPagamento() {
        createPayment.execute(paymentCommand());
    }

    @Quando("a saga pede o link de pagamento e o Mercado Pago falha")
    public void gerarPagamentoComFalha() {
        lastError = capture(() -> createPayment.execute(paymentCommand()));
    }

    @Quando("a verificação periódica consulta os pagamentos pendentes")
    public void verificarPagamentos() {
        verifyPayments.execute(new VerifyPendingPaymentsCommand(Duration.ZERO, 50));
    }

    @Quando("o Mercado Pago passa o pagamento para {string}")
    public void mercadoPagoMudaStatus(String status) {
        mercadoPago.orderIds().forEach(orderId -> mercadoPago.changeStatus(orderId, status));
    }

    @Quando("a saga manda cancelar o pagamento")
    public void cancelarPagamento() {
        cancelPayment.execute(new CancelMercadoPagoPaymentCommand(workOrderId));
    }

    @Quando("a saga manda estornar o pagamento")
    public void estornarPagamento() {
        refundPayment.execute(new RefundMercadoPagoPaymentCommand(workOrderId));
    }

    @Quando("a saga manda cancelar o orçamento")
    public void cancelarOrcamento() {
        cancelBudget.execute(new CancelWorkOrderBudgetCommand(workOrderId));
    }

    // ---------- Então ----------

    @Então("o orçamento da OS está {string} com total de {bigdecimal}")
    public void orcamentoEsta(String status, BigDecimal total) {
        WorkOrderBudget budget = budgetGateway.findById(workOrderId).orElseThrow();
        assertEquals(status, budget.getStatus().name());
        assertEquals(0, total.compareTo(budget.getTotalPrice()), "total do orçamento");
    }

    @Então("o orçamento da OS está {string}")
    public void orcamentoEstaSomenteStatus(String status) {
        assertEquals(status, budgetGateway.findById(workOrderId).orElseThrow().getStatus().name());
    }

    @Então("o Finance publicou o evento {string}")
    public void publicou(String eventType) {
        assertTrue(events().contains(eventType), "eventos publicados: " + events());
    }

    @Então("o Finance não publicou o evento {string}")
    public void naoPublicou(String eventType) {
        assertFalse(events().contains(eventType), "eventos publicados: " + events());
    }

    @Então("os eventos publicados, em ordem, são {string}")
    public void eventosEmOrdem(String expected) {
        assertEquals(List.of(expected.split("\\s*,\\s*")), events());
    }

    @Então("o cliente recebeu um e-mail com os links de decisão do orçamento")
    public void clienteRecebeuEmail() {
        assertEquals(1, emailService.sent().size());
        FakeEmailService.SentEmail email = emailService.sent().get(0);
        assertEquals(CLIENT_EMAIL, email.to());
        assertTrue(email.htmlBody().contains(BASE_URL + "/budget-decision/" + lastToken + "/form?action=APPROVED"));
        assertTrue(email.htmlBody().contains("action=REJECTED"));
        assertTrue(email.htmlBody().contains("action=CHANGES_REQUESTED"));
    }

    @Então("o link de decisão fica marcado como usado")
    public void linkUsado() {
        assertTrue(jdbc.queryForObject("SELECT used FROM budget_decision_tokens WHERE token = ?", Boolean.class, lastToken));
    }

    @Então("a operação é recusada com {string}")
    public void operacaoRecusada(String messageKey) {
        assertNotNull(lastError, "a operacao deveria ter sido recusada");
        assertTrue(lastError instanceof DomainExceptionCore, "erro inesperado: " + lastError);
        assertEquals(messageKey, ((DomainExceptionCore) lastError).getMessage());
    }

    @Então("o pagamento da OS está {string}")
    public void pagamentoEsta(String status) {
        assertEquals(status, jdbc.queryForObject(
                "SELECT status FROM payments WHERE external_reference = ? ORDER BY created_at DESC LIMIT 1",
                String.class, workOrderId.toString()));
    }

    @Então("o link de pagamento foi informado à saga")
    public void linkInformado() {
        String payload = jdbc.queryForObject(
                "SELECT payload::text FROM outbox_messages WHERE event_type = 'PaymentLinkGeneratedEvent' AND aggregate_id = ?",
                String.class, workOrderId);
        assertTrue(payload.contains("https://pagamento.test/ORD-"), payload);
    }

    @E("o orçamento é recalculado e enviado de novo ao cliente")
    public void recalculadoEEnviado() {
        recalcular(new BigDecimal("150.00"));
        assertEquals(null, lastError);
        enviarAoCliente();
    }

    // ---------- apoio ----------

    private void decidir(String action, String observation) {
        decidirComToken(action, observation);
    }

    private void decidirComToken(String action, String observation) {
        decideByToken.execute(new ProcessBudgetDecisionByTokenCommand(lastToken, action, observation));
    }

    private CreateMercadoPagoOrderCommand paymentCommand() {
        return new CreateMercadoPagoOrderCommand("bdd-" + UUID.randomUUID(), workOrderId.toString(), "Pagamento da OS",
                budgetGateway.findById(workOrderId).orElseThrow().getTotalPrice(), CLIENT_EMAIL, "Cliente", "BDD",
                1, null, null, null);
    }

    private List<String> events() {
        return jdbc.queryForList(
                "SELECT event_type FROM outbox_messages WHERE aggregate_id = ? ORDER BY occurred_at, created_at",
                String.class, workOrderId);
    }

    private static Throwable capture(Runnable action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }
}
