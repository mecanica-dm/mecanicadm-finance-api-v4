package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetCanceledEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CancelWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.VoidUseCase;
import org.springframework.transaction.annotation.Transactional;

public class CancelWorkOrderBudgetUseCase implements VoidUseCase<CancelWorkOrderBudgetCommand> {

    private final WorkOrderBudgetGateway gateway;
    private final EventPublisher eventPublisher;

    public CancelWorkOrderBudgetUseCase(WorkOrderBudgetGateway gateway, EventPublisher eventPublisher) {
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(CancelWorkOrderBudgetCommand cmd) {
        WorkOrderBudget budget = gateway.findById(cmd.workOrderId())
                .orElseThrow(BudgetExceptions.BudgetNotFound::new);

        if (budget.getStatus() == WorkOrderBudgetStatus.CANCELED) {
            return;
        }

        budget.cancel();
        gateway.save(budget);

        eventPublisher.publish(new BudgetCanceledEvent(budget.getWorkOrderId()));
    }
}
