package com.mecanicadm.mecanicadm_finance_api.infra.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxStore outboxStore;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public OutboxEventPublisher(OutboxStore outboxStore, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.outboxStore = outboxStore;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(MessageHeader event) {
        if (event == null) {
            return;
        }

        String eventType = event.eventType();
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalStateException("Event without eventType: " + event.getClass().getName());
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "OutboxEventPublisher requires an active transaction. "
                            + "Ensure the use case is annotated with @Transactional.");
        }

        UUID messageId = event.messageId();
        if (messageId == null) {
            throw new IllegalStateException("Event without messageId: " + event.getClass().getName());
        }

        Instant occurredAt = event.ocurredAt() == null ? Instant.now() : event.ocurredAt().toInstant();
        String payload = serialize(event, eventType);

        outboxStore.save(new OutboxMessage(
                messageId,
                eventType,
                event.workOrderId(),
                payload,
                occurredAt
        ));

        meterRegistry.counter("outbox.messages.enqueued").increment();
    }

    private String serialize(MessageHeader event, String eventType) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event " + eventType, e);
        }
    }
}
