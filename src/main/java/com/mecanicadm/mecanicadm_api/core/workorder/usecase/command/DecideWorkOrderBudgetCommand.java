package com.mecanicadm.mecanicadm_api.core.workorder.usecase.command;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;

import java.util.UUID;

public record DecideWorkOrderBudgetCommand(
        UUID workOrderId,
        WorkOrderBudgetStatus decision,
        String observation
) {
}
