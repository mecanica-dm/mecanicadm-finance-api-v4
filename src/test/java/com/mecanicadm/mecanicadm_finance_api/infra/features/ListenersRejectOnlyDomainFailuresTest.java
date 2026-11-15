package com.mecanicadm.mecanicadm_finance_api.infra.features;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.CancelPaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.GeneratePaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.RefundPaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CancelMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.RefundMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CancelMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.RefundMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CancelWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CreateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CancelWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CreateWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging.CancelPaymentIntegrationListener;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging.GeneratePaymentIntegrationListener;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging.RefundPaymentIntegrationListener;
import com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging.CancelBudgetIntegrationListener;
import com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging.GenerateBudgetIntegrationListener;
import com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox.InboundMessageContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListenersRejectOnlyDomainFailuresTest {

    private final RejeitarFinanceCommandUseCase rejeitar = mock(RejeitarFinanceCommandUseCase.class);
    private final UUID workOrderId = UUID.randomUUID();
    private final RuntimeException technical = new IllegalStateException("banco fora");
    private final RuntimeException domain = new BudgetExceptions.BudgetNotFound();

    @AfterEach
    void clearContext() {
        InboundMessageContext.clear();
    }

    private void check(Consumer<RuntimeException> failWith, Runnable invoke, String commandName) {
        failWith.accept(technical);
        assertThatThrownBy(invoke::run).isSameAs(technical);
        verify(rejeitar, never()).execute(any());

        UUID inbound = UUID.randomUUID();
        InboundMessageContext.set(inbound);
        failWith.accept(domain);
        assertThatThrownBy(invoke::run).isSameAs(domain);
        verify(rejeitar).execute(new RejeitarFinanceCommandCommand(workOrderId, commandName, domain, inbound));
    }

    @Test
    @DisplayName("GenerateBudget: falha tecnica nao gera recusa, falha de dominio gera com o id da mensagem recebida")
    void generateBudget() {
        CreateWorkOrderBudgetUseCase useCase = mock(CreateWorkOrderBudgetUseCase.class);
        GenerateBudgetIntegrationListener listener = new GenerateBudgetIntegrationListener(useCase, rejeitar);
        GenerateBudgetIntegrationCommand command = new GenerateBudgetIntegrationCommand(workOrderId, null);

        check(e -> {
            org.mockito.Mockito.reset(useCase);
            doThrow(e).when(useCase).execute(any(CreateWorkOrderBudgetCommand.class));
        }, () -> listener.onMessage(withSnapshot(command)), "GenerateBudgetIntegrationCommand");
    }

    private GenerateBudgetIntegrationCommand withSnapshot(GenerateBudgetIntegrationCommand command) {
        return new GenerateBudgetIntegrationCommand(command.workOrderId(),
                new com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderSnapshot(java.math.BigDecimal.TEN, null, null, null, null, null,
                        java.util.List.of(), java.util.List.of()));
    }

    @Test
    @DisplayName("CancelBudget: falha tecnica nao gera recusa, falha de dominio gera com o id da mensagem recebida")
    void cancelBudget() {
        CancelWorkOrderBudgetUseCase useCase = mock(CancelWorkOrderBudgetUseCase.class);
        CancelBudgetIntegrationListener listener = new CancelBudgetIntegrationListener(useCase, rejeitar);

        check(e -> {
            org.mockito.Mockito.reset(useCase);
            doThrow(e).when(useCase).execute(any(CancelWorkOrderBudgetCommand.class));
        }, () -> listener.onMessage(new CancelBudgetIntegrationCommand(workOrderId)), "CancelBudgetIntegrationCommand");
    }

    @Test
    @DisplayName("GeneratePayment: falha tecnica nao gera recusa, falha de dominio gera com o id da mensagem recebida")
    void generatePayment() {
        CreateMercadoPagoOrderUseCase useCase = mock(CreateMercadoPagoOrderUseCase.class);
        WorkOrderBudgetGateway budgets = mock(WorkOrderBudgetGateway.class);
        when(budgets.findById(workOrderId)).thenReturn(Optional.of(
                com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget.restore(workOrderId, java.math.BigDecimal.TEN,
                        com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus.APPROVED, null)));
        GeneratePaymentIntegrationListener listener = new GeneratePaymentIntegrationListener(useCase, budgets, rejeitar);

        check(e -> {
            org.mockito.Mockito.reset(useCase);
            doThrow(e).when(useCase).execute(any(CreateMercadoPagoOrderCommand.class));
        }, () -> listener.onMessage(new GeneratePaymentIntegrationCommand(workOrderId)), "GeneratePaymentIntegrationCommand");
    }

    @Test
    @DisplayName("CancelPayment: falha tecnica nao gera recusa, falha de dominio gera com o id da mensagem recebida")
    void cancelPayment() {
        CancelMercadoPagoPaymentUseCase useCase = mock(CancelMercadoPagoPaymentUseCase.class);
        CancelPaymentIntegrationListener listener = new CancelPaymentIntegrationListener(useCase, rejeitar);

        check(e -> {
            org.mockito.Mockito.reset(useCase);
            doThrow(e).when(useCase).execute(any(CancelMercadoPagoPaymentCommand.class));
        }, () -> listener.onMessage(new CancelPaymentIntegrationCommand(workOrderId)), "CancelPaymentIntegrationCommand");
    }

    @Test
    @DisplayName("RefundPayment: falha tecnica nao gera recusa, falha de dominio gera com o id da mensagem recebida")
    void refundPayment() {
        RefundMercadoPagoPaymentUseCase useCase = mock(RefundMercadoPagoPaymentUseCase.class);
        RefundPaymentIntegrationListener listener = new RefundPaymentIntegrationListener(useCase, rejeitar);

        check(e -> {
            org.mockito.Mockito.reset(useCase);
            doThrow(e).when(useCase).execute(any(RefundMercadoPagoPaymentCommand.class));
        }, () -> listener.onMessage(new RefundPaymentIntegrationCommand(workOrderId)), "RefundPaymentIntegrationCommand");
    }

    @Test
    @DisplayName("Uma excecao de pagamento tambem e de dominio e gera recusa")
    void paymentExceptionIsDomainFailure() {
        assertThat(new PaymentException.PaymentNotFound()).isInstanceOf(com.mecanicadm.mecanicadm_finance_api.shared.exception.DomainExceptionCore.class);
    }
}
