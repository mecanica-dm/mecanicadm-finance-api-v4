package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.shared.usecase.UseCase;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrder;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.WorkOrderExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.CalculateWorkOrderBudgetCommand;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class CalculateWorkOrderBudgetUseCase implements UseCase<CalculateWorkOrderBudgetCommand, UUID> {
    private final WorkOrderGateway gateway;

    public CalculateWorkOrderBudgetUseCase(WorkOrderGateway gateway) {
        this.gateway = gateway;
    }

    public UUID execute(CalculateWorkOrderBudgetCommand cmd) {
        WorkOrder workOrder = gateway.findById(cmd.workOrderId())
                .orElseThrow(WorkOrderExceptions.NotFound::new);

        BigDecimal totalMaterials = defaultToZero(gateway.sumMaterialsTotalByWorkOrderId(cmd.workOrderId()));
        BigDecimal totalLabor = defaultToZero(gateway.sumLaborTotalByWorkOrderId(cmd.workOrderId()));
        BigDecimal totalPrice = totalMaterials.add(totalLabor);

        workOrder.getBudget().ifPresentOrElse(
                budget -> {
                    budget.updateTotalPrice(totalPrice);
                    gateway.saveBudget(budget);
                },
                () -> {
                    WorkOrderBudget newBudget = WorkOrderBudget.create(workOrder.getId(), totalPrice);
                    gateway.saveBudget(newBudget);
                }
        );

        return workOrder.getId();
    }

    private BigDecimal defaultToZero(BigDecimal value) {
        return Objects.isNull(value) ? BigDecimal.ZERO : value;
    }
}
