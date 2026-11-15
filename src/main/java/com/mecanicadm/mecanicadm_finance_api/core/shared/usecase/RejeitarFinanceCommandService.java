package com.mecanicadm.mecanicadm_finance_api.core.shared.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.FinanceCommandRejectedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RejeitarFinanceCommandService implements RejeitarFinanceCommandUseCase {

    private final EventPublisher eventPublisher;

    public RejeitarFinanceCommandService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(RejeitarFinanceCommandCommand cmd) {
        eventPublisher.publish(new FinanceCommandRejectedEvent(
                cmd.workOrderId(),
                cmd.eventType(),
                cmd.exception().getClass().getSimpleName(),
                cmd.exception().getMessage(),
                rejectionMessageId(cmd)));
    }

    private UUID rejectionMessageId(RejeitarFinanceCommandCommand cmd) {
        if (cmd.inboundMessageId() == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes((cmd.inboundMessageId() + ":" + cmd.eventType() + ":rejected").getBytes(StandardCharsets.UTF_8));
    }
}
