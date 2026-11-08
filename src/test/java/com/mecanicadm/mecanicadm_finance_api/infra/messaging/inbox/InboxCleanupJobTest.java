package com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.InboxGateway;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxCleanupJobTest {

    @Mock
    private InboxGateway inboxGateway;

    private InboxProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private InboxCleanupJob job;

    @BeforeEach
    void setUp() {
        properties = new InboxProperties();
        properties.setRetentionDays(30);
        meterRegistry = new SimpleMeterRegistry();
        job = new InboxCleanupJob(inboxGateway, properties, meterRegistry);
    }

    @Test
    @DisplayName("Deve remover mensagens processadas antes do cutoff de retenção")
    void shouldCleanupProcessedMessages() {
        when(inboxGateway.deleteProcessedBefore(any(Instant.class))).thenReturn(2);

        job.cleanupProcessed();

        verify(inboxGateway).deleteProcessedBefore(any(Instant.class));
        assertThat(meterRegistry.counter("inbox.cleanup.deleted").count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Não deve limpar a inbox quando a retenção estiver desabilitada")
    void shouldSkipCleanupWhenRetentionDisabled() {
        properties.setRetentionDays(0);

        job.cleanupProcessed();

        verifyNoInteractions(inboxGateway);
        verify(inboxGateway, never()).deleteProcessedBefore(any());
    }
}
