package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.BudgetDecisionToken;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetSentForDecisionEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetSentForDecisionFailedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetDecisionTokenGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class BudgetSendTransactionService {

    private final WorkOrderBudgetGateway gateway;
    private final BudgetDecisionTokenGateway tokenGateway;
    private final EventPublisher eventPublisher;

    public BudgetSendTransactionService(WorkOrderBudgetGateway gateway,
                                        BudgetDecisionTokenGateway tokenGateway,
                                        EventPublisher eventPublisher) {
        this.gateway = gateway;
        this.tokenGateway = tokenGateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PreparedSend prepare(UUID workOrderId) {
        WorkOrderBudget budget = gateway.findById(workOrderId)
                .orElseThrow(BudgetExceptions.BudgetNotFound::new);

        budget.send();
        gateway.save(budget);

        BudgetDecisionToken token = BudgetDecisionToken.create(budget.getWorkOrderId());
        tokenGateway.create(token);

        eventPublisher.publish(new BudgetSentForDecisionEvent(budget.getWorkOrderId()));

        return new PreparedSend(budget, token);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(UUID workOrderId) {
        eventPublisher.publish(new BudgetSentForDecisionFailedEvent(workOrderId));
    }

    public record PreparedSend(WorkOrderBudget budget, BudgetDecisionToken token) {
    }
}
