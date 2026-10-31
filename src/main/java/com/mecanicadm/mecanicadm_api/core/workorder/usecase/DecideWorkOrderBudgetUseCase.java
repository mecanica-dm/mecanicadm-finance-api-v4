package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetApprovedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetChangesRequestedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetRejectedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.DecideWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_api.shared.usecase.VoidUseCase;
import org.springframework.util.StringUtils;

public class DecideWorkOrderBudgetUseCase implements VoidUseCase<DecideWorkOrderBudgetCommand> {

    private final WorkOrderBudgetGateway gateway;
    private final EventPublisher eventPublisher;

    public DecideWorkOrderBudgetUseCase(WorkOrderBudgetGateway gateway, EventPublisher eventPublisher) {
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
    }

    public void execute(DecideWorkOrderBudgetCommand cmd) {
        WorkOrderBudget budget = gateway.findById(cmd.workOrderId())
                .orElseThrow(BudgetExceptions.BudgetNotFound::new);

        if (budget.getStatus() != WorkOrderBudgetStatus.WAITING_DECISION) {
            throw new BudgetExceptions.BudgetNotWaitingDecision();
        }

        applyDecision(budget, cmd);
        gateway.save(budget);
        publishDecision(budget, cmd);
    }

    private void applyDecision(WorkOrderBudget budget, DecideWorkOrderBudgetCommand cmd) {
        switch (cmd.decision()) {
            case APPROVED -> budget.approve(cmd.observation());
            case REJECTED -> {
                validateObservation(cmd.observation());
                budget.reject(cmd.observation(), false);
            }
            case CHANGES_REQUESTED -> {
                validateObservation(cmd.observation());
                budget.reject(cmd.observation(), true);
            }
            default -> throw new BudgetExceptions.BudgetDecisionInvalid(cmd.decision().name());
        }
    }

    private void publishDecision(WorkOrderBudget budget, DecideWorkOrderBudgetCommand cmd) {
        switch (cmd.decision()) {
            case APPROVED -> eventPublisher.publish(new BudgetApprovedEvent(budget.getWorkOrderId()));
            case REJECTED -> eventPublisher.publish(
                    new BudgetRejectedEvent(budget.getWorkOrderId(), cmd.observation()));
            case CHANGES_REQUESTED -> eventPublisher.publish(
                    new BudgetChangesRequestedEvent(budget.getWorkOrderId(), cmd.observation()));
            default -> {
                // Unreachable: applyDecision already validated the decision.
            }
        }
    }

    private void validateObservation(String observation) {
        if (!StringUtils.hasText(observation)) {
            throw new BudgetExceptions.BudgetObservationRequired();
        }
    }
}
