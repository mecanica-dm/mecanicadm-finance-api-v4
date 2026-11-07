package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetApprovedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetChangesRequestedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetRejectedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.DecideWorkOrderBudgetCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecideWorkOrderBudgetUseCaseTest {

    @Mock
    private WorkOrderBudgetGateway gateway;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private DecideWorkOrderBudgetUseCase useCase;

    @Test
    @DisplayName("Deve aprovar orçamento e publicar evento")
    void shouldApproveBudget() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = waitingDecisionBudget(workOrderId);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new DecideWorkOrderBudgetCommand(workOrderId, WorkOrderBudgetStatus.APPROVED, "ok"));

        assertEquals(WorkOrderBudgetStatus.APPROVED, budget.getStatus());
        assertEquals("ok", budget.getObservation());
        verify(gateway).save(budget);
        verify(eventPublisher).publish(any(BudgetApprovedEvent.class));
    }

    @Test
    @DisplayName("Deve rejeitar orçamento e publicar evento")
    void shouldRejectBudget() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = waitingDecisionBudget(workOrderId);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new DecideWorkOrderBudgetCommand(workOrderId, WorkOrderBudgetStatus.REJECTED, "caro"));

        assertEquals(WorkOrderBudgetStatus.REJECTED, budget.getStatus());
        assertEquals("caro", budget.getObservation());
        verify(gateway).save(budget);
        verify(eventPublisher).publish(any(BudgetRejectedEvent.class));
    }

    @Test
    @DisplayName("Deve solicitar alterações e publicar evento")
    void shouldRequestChanges() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = waitingDecisionBudget(workOrderId);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new DecideWorkOrderBudgetCommand(
                workOrderId, WorkOrderBudgetStatus.CHANGES_REQUESTED, "ajustar"));

        assertEquals(WorkOrderBudgetStatus.CHANGES_REQUESTED, budget.getStatus());
        assertEquals("ajustar", budget.getObservation());
        verify(gateway).save(budget);
        verify(eventPublisher).publish(any(BudgetChangesRequestedEvent.class));
    }

    @Test
    @DisplayName("Deve exigir observação ao rejeitar")
    void shouldRequireObservationWhenRejecting() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = waitingDecisionBudget(workOrderId);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        assertThrows(BudgetExceptions.BudgetObservationRequired.class,
                () -> useCase.execute(new DecideWorkOrderBudgetCommand(
                        workOrderId, WorkOrderBudgetStatus.REJECTED, " ")));

        verify(gateway, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o orçamento não estiver aguardando decisão")
    void shouldThrowWhenBudgetNotWaitingDecision() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = WorkOrderBudget.restore(
                workOrderId, BigDecimal.TEN, WorkOrderBudgetStatus.PENDING, null);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        assertThrows(BudgetExceptions.BudgetNotWaitingDecision.class,
                () -> useCase.execute(new DecideWorkOrderBudgetCommand(
                        workOrderId, WorkOrderBudgetStatus.APPROVED, null)));

        verify(gateway, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção para decisão inválida")
    void shouldThrowForInvalidDecision() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = waitingDecisionBudget(workOrderId);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        assertThrows(BudgetExceptions.BudgetDecisionInvalid.class,
                () -> useCase.execute(new DecideWorkOrderBudgetCommand(
                        workOrderId, WorkOrderBudgetStatus.PENDING, null)));

        verify(gateway, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o orçamento não for encontrado")
    void shouldThrowWhenBudgetNotFound() {
        UUID workOrderId = UUID.randomUUID();
        when(gateway.findById(workOrderId)).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetNotFound.class,
                () -> useCase.execute(new DecideWorkOrderBudgetCommand(
                        workOrderId, WorkOrderBudgetStatus.APPROVED, null)));
    }

    private WorkOrderBudget waitingDecisionBudget(UUID workOrderId) {
        return WorkOrderBudget.restore(
                workOrderId, new BigDecimal("100.00"), WorkOrderBudgetStatus.WAITING_DECISION, null);
    }
}
