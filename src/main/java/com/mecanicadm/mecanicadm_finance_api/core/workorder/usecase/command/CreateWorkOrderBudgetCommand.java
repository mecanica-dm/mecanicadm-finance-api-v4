package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderCreatedEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateWorkOrderBudgetCommand(
        UUID workOrderId,
        BigDecimal totalPrice,
        WorkOrderBudgetStatus status,
        String observation,
        String description,
        WorkOrderCreatedEvent.Client client,
        WorkOrderCreatedEvent.Vehicle vehicle,
        List<WorkOrderCreatedEvent.MaterialItem> materialItems,
        List<WorkOrderCreatedEvent.LaborItem> laborItems
) {
}
