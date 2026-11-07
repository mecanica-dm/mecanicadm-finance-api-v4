package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetCanceledEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CancelWorkOrderBudgetCommand;
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
class CancelWorkOrderBudgetUseCaseTest {

    @Mock
    private WorkOrderBudgetGateway gateway;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CancelWorkOrderBudgetUseCase useCase;

    @Test
    @DisplayName("Deve cancelar o orçamento e publicar evento")
    void shouldCancelBudget() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = WorkOrderBudget.restore(
                workOrderId, BigDecimal.TEN, WorkOrderBudgetStatus.PENDING, null);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new CancelWorkOrderBudgetCommand(workOrderId));

        assertEquals(WorkOrderBudgetStatus.CANCELED, budget.getStatus());
        verify(gateway).save(budget);
        verify(eventPublisher).publish(any(BudgetCanceledEvent.class));
    }

    @Test
    @DisplayName("Deve ignorar cancelamento de orçamento já cancelado")
    void shouldIgnoreAlreadyCanceledBudget() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = WorkOrderBudget.restore(
                workOrderId, BigDecimal.TEN, WorkOrderBudgetStatus.CANCELED, null);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new CancelWorkOrderBudgetCommand(workOrderId));

        verify(gateway, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o orçamento não for encontrado")
    void shouldThrowWhenBudgetNotFound() {
        UUID workOrderId = UUID.randomUUID();
        when(gateway.findById(workOrderId)).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetNotFound.class,
                () -> useCase.execute(new CancelWorkOrderBudgetCommand(workOrderId)));
    }
}
