package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(
        UUID id,
        String eventType,
        UUID aggregateId,
        String payload,
        Instant occurredAt
) {
}
