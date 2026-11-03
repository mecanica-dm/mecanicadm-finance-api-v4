package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

import com.mecanicadm.mecanicadm_api.infra.messaging.rabbit.RabbitEventSender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxStore outboxStore;
    @Mock
    private RabbitEventSender eventSender;

    private OutboxProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        properties = new OutboxProperties();
        properties.setBatchSize(10);
        properties.setProcessingTimeoutSeconds(60);
        properties.setRetentionDays(30);
        meterRegistry = new SimpleMeterRegistry();
        relay = new OutboxRelay(outboxStore, eventSender, properties, meterRegistry);
    }

    @Test
    @DisplayName("Deve publicar mensagem reivindicada e marcá-la como publicada")
    void shouldPublishClaimedMessage() {
        OutboxMessage message = message();
        when(outboxStore.claim(10)).thenReturn(List.of(message));

        relay.publishPending();

        verify(eventSender).send(message);
        verify(outboxStore).markPublished(message.id());
        verify(outboxStore, never()).markFailed(any(), anyString());
        assertThat(meterRegistry.counter("outbox.messages.published").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve marcar como FAILED na primeira falha, sem retry")
    void shouldMarkFailedOnFirstFailure() {
        OutboxMessage message = message();
        when(outboxStore.claim(10)).thenReturn(List.of(message));
        doThrow(new RuntimeException("rabbit down")).when(eventSender).send(message);

        relay.publishPending();

        verify(outboxStore).markFailed(eq(message.id()), contains("rabbit down"));
        verify(outboxStore, never()).markPublished(any());
        verify(eventSender, times(1)).send(message);
        verify(outboxStore, times(1)).claim(10);
        assertThat(meterRegistry.counter("outbox.messages.failed").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve marcar PROCESSING expirado como FAILED")
    void shouldFailStaleProcessing() {
        when(outboxStore.moveStaleProcessingToFailed(Duration.ofSeconds(60))).thenReturn(2);
        when(outboxStore.claim(10)).thenReturn(List.of());

        relay.publishPending();

        verify(outboxStore).moveStaleProcessingToFailed(Duration.ofSeconds(60));
        assertThat(meterRegistry.counter("outbox.processing.stale").count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve remover apenas mensagens publicadas antes do cutoff de retenção")
    void shouldCleanupPublishedMessages() {
        relay.cleanupPublished();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(outboxStore).deletePublishedBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(Instant.now());
    }

    @Test
    @DisplayName("Não deve limpar a outbox quando a retenção estiver desabilitada")
    void shouldSkipCleanupWhenRetentionDisabled() {
        properties.setRetentionDays(0);

        relay.cleanupPublished();

        verify(outboxStore, never()).deletePublishedBefore(any());
    }

    private OutboxMessage message() {
        return new OutboxMessage(
                UUID.randomUUID(),
                "BudgetApprovedEvent",
                UUID.randomUUID(),
                "{\"workOrderId\":\"123\"}",
                Instant.now());
    }
}
