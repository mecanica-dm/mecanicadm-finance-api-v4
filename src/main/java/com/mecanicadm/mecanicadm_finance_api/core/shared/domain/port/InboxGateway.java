package com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.InboxEntry;

import java.time.Instant;

public interface InboxGateway {

    /**
     * Reserva a mensagem para processamento.
     *
     * @return {@code true} se a mensagem ainda não foi processada por este consumidor;
     * {@code false} se já existe um registro para a chave (messageId, consumer).
     */
    boolean tryStart(InboxEntry entry);

    void markProcessed(InboxEntry entry);

    int deleteProcessedBefore(Instant cutoff);
}
