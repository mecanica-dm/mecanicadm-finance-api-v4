package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudgetItem;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetGeneratedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.CreateWorkOrderBudgetCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateWorkOrderBudgetUseCaseTest {

    @Mock
    private WorkOrderBudgetGateway gateway;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CreateWorkOrderBudgetUseCase useCase;

    @Test
    @DisplayName("Deve criar orçamento a partir dos itens do evento")
    void shouldCreateBudgetFromEventItems() {
        UUID workOrderId = UUID.randomUUID();
        CreateWorkOrderBudgetCommand command = command(workOrderId, null);

        useCase.execute(command);

        ArgumentCaptor<WorkOrderBudget> captor = ArgumentCaptor.forClass(WorkOrderBudget.class);
        verify(gateway).save(captor.capture());
        WorkOrderBudget budget = captor.getValue();

        assertEquals(workOrderId, budget.getWorkOrderId());
        assertEquals(new BigDecimal("180.00"), budget.getTotalPrice());
        assertEquals(WorkOrderBudgetStatus.PENDING, budget.getStatus());
        assertEquals("descricao", budget.getDescription());
        assertEquals("Cliente Teste", budget.getInfo().getClient().getName());
        assertEquals("ABC-1234", budget.getInfo().getVehicle().getLicensePlate());
        assertEquals(2, budget.getItems().size());
        verify(eventPublisher).publish(any(BudgetGeneratedEvent.class));
    }

    @Test
    @DisplayName("Deve priorizar o valor total informado")
    void shouldUseInformedTotalPrice() {
        UUID workOrderId = UUID.randomUUID();

        useCase.execute(command(workOrderId, new BigDecimal("999.00")));

        ArgumentCaptor<WorkOrderBudget> captor = ArgumentCaptor.forClass(WorkOrderBudget.class);
        verify(gateway).save(captor.capture());
        assertEquals(new BigDecimal("999.00"), captor.getValue().getTotalPrice());
    }

    @Test
    @DisplayName("Deve criar itens de material e serviço com os tipos corretos")
    void shouldCreateItemsWithCorrectTypes() {
        UUID workOrderId = UUID.randomUUID();

        useCase.execute(command(workOrderId, null));

        ArgumentCaptor<WorkOrderBudget> captor = ArgumentCaptor.forClass(WorkOrderBudget.class);
        verify(gateway).save(captor.capture());
        assertNotNull(captor.getValue().getItems().stream()
                .filter(item -> WorkOrderBudgetItem.TYPE_MATERIAL.equals(item.getType()))
                .findFirst()
                .orElse(null));
        assertNotNull(captor.getValue().getItems().stream()
                .filter(item -> WorkOrderBudgetItem.TYPE_LABOR.equals(item.getType()))
                .findFirst()
                .orElse(null));
    }

    private CreateWorkOrderBudgetCommand command(UUID workOrderId, BigDecimal totalPrice) {
        return new CreateWorkOrderBudgetCommand(
                workOrderId,
                totalPrice,
                WorkOrderBudgetStatus.PENDING,
                "observacao",
                "descricao",
                new WorkOrderCreatedEvent.Client(UUID.randomUUID(), "Cliente Teste", "cliente@teste.com",
                        "12345678901", "48999999999"),
                new WorkOrderCreatedEvent.Vehicle("ABC-1234", "Modelo", "Marca", (short) 2020),
                List.of(new WorkOrderCreatedEvent.MaterialItem(
                        UUID.randomUUID(), UUID.randomUUID(), 2, "Material", "Marca",
                        "descricao material", new BigDecimal("50.00"), "PART")),
                List.of(new WorkOrderCreatedEvent.LaborItem(
                        UUID.randomUUID(), workOrderId, UUID.randomUUID(), null, null, null,
                        "Servico", new BigDecimal("80.00")))
        );
    }
}
