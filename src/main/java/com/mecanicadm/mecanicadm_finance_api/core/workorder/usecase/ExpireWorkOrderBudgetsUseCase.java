package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.BudgetDecisionToken;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetExpiredEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetExpiryGateway;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.UseCase;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class ExpireWorkOrderBudgetsUseCase implements UseCase<Integer, Integer> {

    private final BudgetExpiryGateway gateway;
    private final EventPublisher eventPublisher;

    public ExpireWorkOrderBudgetsUseCase(BudgetExpiryGateway gateway, EventPublisher eventPublisher) {
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Integer execute(Integer batchSize) {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusHours(BudgetDecisionToken.VALIDITY_HOURS);
        List<BudgetExpiryGateway.ExpiredBudget> expired = gateway.claimExpired(cutoff, batchSize);
        expired.forEach(budget -> {
            eventPublisher.publish(new BudgetExpiredEvent(budget.workOrderId()));
            gateway.markExpiryNotified(budget.tokenId());
        });
        return expired.size();
    }
}
