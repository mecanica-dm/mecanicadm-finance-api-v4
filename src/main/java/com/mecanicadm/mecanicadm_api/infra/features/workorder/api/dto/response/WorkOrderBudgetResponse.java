package com.mecanicadm.mecanicadm_api.infra.features.workorder.api.dto.response;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;

import java.math.BigDecimal;

public record WorkOrderBudgetResponse(
        BigDecimal totalPrice,
        WorkOrderBudgetStatus status,
        String observation
) {
    public static WorkOrderBudgetResponse from(WorkOrderBudget budget) {
        return new WorkOrderBudgetResponse(
                budget.getTotalPrice(),
                budget.getStatus(),
                budget.getObservation()
        );
    }
}
