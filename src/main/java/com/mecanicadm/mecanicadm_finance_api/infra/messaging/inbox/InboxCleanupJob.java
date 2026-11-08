package com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.InboxGateway;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@ConditionalOnProperty(prefix = "app.inbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InboxCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(InboxCleanupJob.class);

    private final InboxGateway inboxGateway;
    private final InboxProperties properties;
    private final MeterRegistry meterRegistry;

    public InboxCleanupJob(InboxGateway inboxGateway,
                           InboxProperties properties,
                           MeterRegistry meterRegistry) {
        this.inboxGateway = inboxGateway;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "${app.inbox.cleanup-cron:0 30 3 * * *}")
    public void cleanupProcessed() {
        if (properties.getRetentionDays() <= 0) {
            return;
        }

        try {
            Instant cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
            int deleted = inboxGateway.deleteProcessedBefore(cutoff);
            if (deleted > 0) {
                meterRegistry.counter("inbox.cleanup.deleted").increment(deleted);
                logger.info("Removidas {} mensagens processadas da inbox com processed_at anterior a {}",
                        deleted, cutoff);
            }
        } catch (RuntimeException e) {
            logger.error("Erro ao limpar mensagens processadas da inbox", e);
        }
    }
}
