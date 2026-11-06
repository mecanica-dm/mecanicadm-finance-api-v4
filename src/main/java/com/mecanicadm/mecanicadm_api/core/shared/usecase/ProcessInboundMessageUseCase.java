package com.mecanicadm.mecanicadm_api.core.shared.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.InboxGateway;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

public class ProcessInboundMessageUseCase {

    private final InboxGateway inboxGateway;

    public ProcessInboundMessageUseCase(InboxGateway inboxGateway) {
        this.inboxGateway = inboxGateway;
    }

    /**
     * Executa o handler uma única vez para a chave (messageId, consumer).
     * <p>
     * A reserva na inbox, a regra de negócio e a marcação como processada participam da mesma transação.
     * Uma falha no handler provoca rollback, removendo a reserva e permitindo nova tentativa.
     *
     * @return {@code true} quando a mensagem foi processada; {@code false} quando era duplicada.
     */
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
