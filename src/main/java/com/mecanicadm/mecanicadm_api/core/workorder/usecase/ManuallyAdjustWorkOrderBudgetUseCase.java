package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetUpdatedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.ManuallyAdjustWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_api.shared.usecase.VoidUseCase;
import org.springframework.transaction.annotation.Transactional;

public class ManuallyAdjustWorkOrderBudgetUseCase implements VoidUseCase<ManuallyAdjustWorkOrderBudgetCommand> {

    private final WorkOrderBudgetGateway gateway;
    private final EventPublisher eventPublisher;

    public ManuallyAdjustWorkOrderBudgetUseCase(WorkOrderBudgetGateway gateway, EventPublisher eventPublisher) {
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ManuallyAdjustWorkOrderBudgetCommand cmd) {
        WorkOrderBudget budget = gateway.findById(cmd.workOrderId())
                .orElseThrow(BudgetExceptions.BudgetNotFound::new);

        budget.updateTotalPrice(cmd.newTotalPrice());
        gateway.save(budget);

        eventPublisher.publish(new BudgetUpdatedEvent(
                budget.getWorkOrderId(),
                budget.getTotalPrice(),
                budget.getObservation()
        ));
    }
}
