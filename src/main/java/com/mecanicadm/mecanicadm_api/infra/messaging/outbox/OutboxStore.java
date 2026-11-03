package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxStore {

    void save(OutboxMessage message);

    List<OutboxMessage> claim(int batchSize);

    void markPublished(UUID id);

    void markFailed(UUID id, String error);

    int moveStaleProcessingToFailed(Duration timeout);

    int deletePublishedBefore(Instant cutoff);

    long countByStatus(OutboxStatus status);

    long oldestPendingAgeSeconds();
}
