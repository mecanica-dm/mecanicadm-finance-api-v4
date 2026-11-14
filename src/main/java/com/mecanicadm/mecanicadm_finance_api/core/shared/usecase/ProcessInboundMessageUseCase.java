package com.mecanicadm.mecanicadm_finance_api.core.shared.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.InboxGateway;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

public class ProcessInboundMessageUseCase {

    private final InboxGateway inboxGateway;

    public ProcessInboundMessageUseCase(InboxGateway inboxGateway) {
        this.inboxGateway = inboxGateway;
    }


    @Transactional
    public boolean execute(InboxEntry entry, Runnable handler) {
        requireNonNull(entry, "entry");
        requireNonNull(handler, "handler");

        if (!inboxGateway.tryStart(entry)) {
            return false;
        }

        handler.run();
        inboxGateway.markProcessed(entry);
        return true;
    }
}
