package com.mecanicadm.mecanicadm_finance_api.core.shared.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.FinanceCommandRejected;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class RejeitarFinanceCommandService implements RejeitarFinanceCommandUseCase {

    private final EventPublisher eventPublisher;

    public RejeitarFinanceCommandService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(RejeitarFinanceCommandCommand cmd) {
        eventPublisher.publish(new FinanceCommandRejected(
                cmd.workOrderId(),
                cmd.eventType(),
                cmd.exception().getClass().getSimpleName(),
                cmd.exception().getMessage()));
    }
}
