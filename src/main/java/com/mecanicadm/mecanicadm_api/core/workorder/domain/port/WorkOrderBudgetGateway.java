package com.mecanicadm.mecanicadm_api.core.workorder.domain.port;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderBudgetGateway {
    void save(WorkOrderBudget budget);

    Optional<WorkOrderBudget> findById(UUID workOrderId);
}
