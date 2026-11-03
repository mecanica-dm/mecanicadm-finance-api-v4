package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

import com.mecanicadm.mecanicadm_api.infra.messaging.rabbit.RabbitEventSender;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int MAX_ERROR_LENGTH = 4000;

    private final OutboxStore outboxStore;
    private final RabbitEventSender eventSender;
    private final OutboxProperties properties;
    private final MeterRegistry meterRegistry;

    public OutboxRelay(OutboxStore outboxStore,
                       RabbitEventSender eventSender,
                       OutboxProperties properties,
                       MeterRegistry meterRegistry) {
        this.outboxStore = outboxStore;
        this.eventSender = eventSender;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(
            initialDelayString = "${app.outbox.initial-delay-ms:5000}",
            fixedDelayString = "${app.outbox.poll-interval-ms:1000}"
    )
    public void publishPending() {
        try {
            failStaleProcessing();

            List<OutboxMessage> messages = outboxStore.claim(properties.getBatchSize());
            for (OutboxMessage message : messages) {
                publish(message);
            }
        } catch (RuntimeException e) {
            logger.error("Erro no ciclo de publicação da outbox", e);
        }
    }

    @Scheduled(cron = "${app.outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanupPublished() {
        if (properties.getRetentionDays() <= 0) {
            return;
        }

        try {
            Instant cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
            int deleted = outboxStore.deletePublishedBefore(cutoff);
            if (deleted > 0) {
                logger.info("Removidas {} mensagens publicadas da outbox com published_at anterior a {}",
                        deleted, cutoff);
            }
        } catch (RuntimeException e) {
            logger.error("Erro ao limpar mensagens publicadas da outbox", e);
        }
    }

    private void failStaleProcessing() {
        int stale = outboxStore.moveStaleProcessingToFailed(
                Duration.ofSeconds(properties.getProcessingTimeoutSeconds()));
        if (stale > 0) {
            meterRegistry.counter("outbox.processing.stale").increment(stale);
            logger.warn("{} mensagens em PROCESSING excederam o timeout e foram marcadas como FAILED", stale);
        }
    }

    private void publish(OutboxMessage message) {
        try {
            eventSender.send(message);
            outboxStore.markPublished(message.id());
            meterRegistry.counter("outbox.messages.published").increment();
            logger.info("Evento {} publicado (messageId={}, workOrderId={})",
                    message.eventType(), message.id(), message.aggregateId());
        } catch (RuntimeException e) {
            String error = errorSummary(e);
            markFailed(message, error);
            meterRegistry.counter("outbox.messages.failed").increment();
            logger.error("Falha ao publicar evento {} (messageId={}, workOrderId={}): {}",
                    message.eventType(), message.id(), message.aggregateId(), error, e);
        }
    }

    private void markFailed(OutboxMessage message, String error) {
        try {
            outboxStore.markFailed(message.id(), error);
        } catch (RuntimeException e) {
            logger.error("Falha ao marcar mensagem {} como FAILED no banco", message.id(), e);
        }
    }

    private String errorSummary(Throwable error) {
        String message = error.getMessage();
        String summary = message == null || message.isBlank()
                ? error.getClass().getName()
                : error.getClass().getSimpleName() + ": " + message;
        return summary.length() <= MAX_ERROR_LENGTH
                ? summary
                : summary.substring(0, MAX_ERROR_LENGTH);
    }
}
