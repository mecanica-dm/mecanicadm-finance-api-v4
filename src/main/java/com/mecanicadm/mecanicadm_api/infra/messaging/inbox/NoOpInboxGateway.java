package com.mecanicadm.mecanicadm_api.infra.messaging.inbox;

import com.mecanicadm.mecanicadm_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.InboxGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "app.inbox", name = "enabled", havingValue = "false")
public class NoOpInboxGateway implements InboxGateway {

    @Override
    public boolean tryStart(InboxEntry entry) {
        return true;
    }

    @Override
    public void markProcessed(InboxEntry entry) {
        // Intencionalmente vazio: inbox desabilitada.
    }

    @Override
    public int deleteProcessedBefore(Instant cutoff) {
        return 0;
    }
}
