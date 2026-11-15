package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.GeneratePaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratePaymentIntegrationListenerTest {

    @Mock
    private CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase;
    @Mock
    private WorkOrderBudgetGateway budgetGateway;
    @Mock
    private RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    private GeneratePaymentIntegrationListener listener;

    @BeforeEach
    void setUp() {
        listener = new GeneratePaymentIntegrationListener(createMercadoPagoOrderUseCase, budgetGateway,
                rejeitarFinanceCommandUseCase);
    }

    private WorkOrderBudget budget(UUID workOrderId) {
        WorkOrderBudgetInfo info = new WorkOrderBudgetInfo(
                WorkOrderBudgetClient.restore(UUID.randomUUID(), "Maria da Silva Souza", "maria@teste.com", "123", "999"),
                null, "Revisao geral", Set.of());
        return WorkOrderBudget.restore(workOrderId, new BigDecimal("720.00"), WorkOrderBudgetStatus.APPROVED, null, info);
    }

    @Test
    @DisplayName("Deve criar o pagamento com o valor do orcamento e os dados do cliente, usando a OS como chave de idempotencia")
    void shouldCreatePaymentWithBudgetAmountAndClient() {
        UUID workOrderId = UUID.randomUUID();
        when(budgetGateway.findById(workOrderId)).thenReturn(Optional.of(budget(workOrderId)));

        listener.onMessage(new GeneratePaymentIntegrationCommand(workOrderId));

        verify(createMercadoPagoOrderUseCase).execute(new CreateMercadoPagoOrderCommand(
                workOrderId.toString(), workOrderId.toString(), "Revisao geral", new BigDecimal("720.00"),
                "maria@teste.com", "Maria", "da Silva Souza", null, null, null, null));
        verifyNoInteractions(rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve rejeitar o command e propagar a excecao quando nao houver orcamento da OS")
    void shouldRejectWhenBudgetDoesNotExist() {
        UUID workOrderId = UUID.randomUUID();
        when(budgetGateway.findById(workOrderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.onMessage(new GeneratePaymentIntegrationCommand(workOrderId)))
                .isInstanceOf(BudgetExceptions.BudgetNotFound.class);

        verify(rejeitarFinanceCommandUseCase).execute(any(RejeitarFinanceCommandCommand.class));
        verifyNoInteractions(createMercadoPagoOrderUseCase);
    }

    @Test
    @DisplayName("Deve publicar evento de rejeição e propagar a exceção do serviço")
    void shouldPublishRejectionAndRethrowWhenServiceFails() {
        UUID workOrderId = UUID.randomUUID();
        when(budgetGateway.findById(workOrderId)).thenReturn(Optional.of(budget(workOrderId)));
        PaymentException.PaymentIdempotencyKeyInvalid failure = new PaymentException.PaymentIdempotencyKeyInvalid();
        doThrow(failure).when(createMercadoPagoOrderUseCase).execute(any(CreateMercadoPagoOrderCommand.class));

        assertThatThrownBy(() -> listener.onMessage(new GeneratePaymentIntegrationCommand(workOrderId)))
                .isSameAs(failure);

        verify(rejeitarFinanceCommandUseCase).execute(new RejeitarFinanceCommandCommand(
                workOrderId, GeneratePaymentIntegrationCommand.class.getSimpleName(), failure));
    }
}
