package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetItem;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetGeneratedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CalculateWorkOrderBudgetCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateWorkOrderBudgetUseCaseTest {

    @Mock
    private WorkOrderBudgetGateway gateway;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CalculateWorkOrderBudgetUseCase useCase;

    @Test
    @DisplayName("Deve recalcular o orçamento existente somando os itens")
    void shouldRecalculateExistingBudgetFromItems() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = WorkOrderBudget.restore(
                workOrderId,
                new BigDecimal("10.00"),
                WorkOrderBudgetStatus.APPROVED,
                "observacao",
                new WorkOrderBudgetInfo(null, null, "descricao", Set.of(
                        WorkOrderBudgetItem.create("Material", 2, new BigDecimal("50.00"), WorkOrderBudgetItem.TYPE_MATERIAL),
                        WorkOrderBudgetItem.create("Servico", 1, new BigDecimal("80.00"), WorkOrderBudgetItem.TYPE_LABOR)
                )));
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        UUID result = useCase.execute(new CalculateWorkOrderBudgetCommand(workOrderId, null));

        assertEquals(workOrderId, result);
        assertEquals(new BigDecimal("180.00"), budget.getTotalPrice());
        assertEquals(WorkOrderBudgetStatus.PENDING, budget.getStatus());
        verify(gateway).save(budget);
        verify(eventPublisher).publish(any(BudgetGeneratedEvent.class));
    }

    @Test
    @DisplayName("Deve usar o valor informado quando presente")
    void shouldUseInformedTotalPrice() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = WorkOrderBudget.restore(
                workOrderId, new BigDecimal("100.00"), WorkOrderBudgetStatus.PENDING, null);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new CalculateWorkOrderBudgetCommand(workOrderId, new BigDecimal("500.00")));

        assertEquals(new BigDecimal("500.00"), budget.getTotalPrice());
        verify(gateway).save(budget);
    }

    @Test
    @DisplayName("Deve criar orçamento quando não existir e o valor for informado")
    void shouldCreateBudgetWhenMissingAndTotalPriceInformed() {
        UUID workOrderId = UUID.randomUUID();
        when(gateway.findById(workOrderId)).thenReturn(Optional.empty());

        UUID result = useCase.execute(new CalculateWorkOrderBudgetCommand(workOrderId, new BigDecimal("250.00")));

        assertEquals(workOrderId, result);
        ArgumentCaptor<WorkOrderBudget> captor = ArgumentCaptor.forClass(WorkOrderBudget.class);
        verify(gateway).save(captor.capture());
        assertEquals(new BigDecimal("250.00"), captor.getValue().getTotalPrice());
        assertEquals(WorkOrderBudgetStatus.PENDING, captor.getValue().getStatus());
        verify(eventPublisher).publish(any(BudgetGeneratedEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o orçamento não existir e nenhum valor for informado")
    void shouldThrowWhenBudgetMissingAndNoTotalPrice() {
        UUID workOrderId = UUID.randomUUID();
        when(gateway.findById(workOrderId)).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetNotFound.class,
                () -> useCase.execute(new CalculateWorkOrderBudgetCommand(workOrderId, null)));

        verify(gateway, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
