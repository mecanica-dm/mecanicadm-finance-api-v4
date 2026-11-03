package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetUpdatedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.ManuallyAdjustWorkOrderBudgetCommand;
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
class ManuallyAdjustWorkOrderBudgetUseCaseTest {

    @Mock
    private WorkOrderBudgetGateway gateway;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ManuallyAdjustWorkOrderBudgetUseCase useCase;

    @Test
    @DisplayName("Deve ajustar o orçamento manualmente e publicar evento")
    void shouldAdjustBudgetSuccessfully() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = WorkOrderBudget.restore(
                workOrderId, new BigDecimal("100.00"), WorkOrderBudgetStatus.APPROVED, "ok");
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new ManuallyAdjustWorkOrderBudgetCommand(workOrderId, new BigDecimal("350.00")));

        assertEquals(new BigDecimal("350.00"), budget.getTotalPrice());
        assertEquals(WorkOrderBudgetStatus.PENDING, budget.getStatus());
        verify(gateway).save(budget);
        verify(eventPublisher).publish(any(BudgetUpdatedEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o orçamento não for encontrado")
    void shouldThrowExceptionWhenBudgetNotFound() {
        UUID workOrderId = UUID.randomUUID();
        when(gateway.findById(workOrderId)).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetNotFound.class,
                () -> useCase.execute(new ManuallyAdjustWorkOrderBudgetCommand(workOrderId, BigDecimal.TEN)));

        verify(gateway, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
