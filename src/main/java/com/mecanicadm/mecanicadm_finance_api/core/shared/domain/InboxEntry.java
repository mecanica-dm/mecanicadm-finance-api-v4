package com.mecanicadm.mecanicadm_finance_api.core.shared.domain;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record InboxEntry(
        UUID messageId,
        String consumer,
        String eventType,
        UUID aggregateId,
        Instant receivedAt
) {

    public InboxEntry {
        requireNonNull(messageId, "messageId");
        requireNonNull(consumer, "consumer");
        requireNonNull(eventType, "eventType");
        receivedAt = receivedAt != null ? receivedAt : Instant.now();
    }
}
