package com.mecanicadm.mecanicadm_finance_api.infra.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetApprovedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxStore outboxStore;

    private SimpleMeterRegistry meterRegistry;
    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        meterRegistry = new SimpleMeterRegistry();
        publisher = new OutboxEventPublisher(outboxStore, objectMapper, meterRegistry);
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("Deve gravar o evento na outbox dentro da transação ativa")
    void shouldSaveEventInOutbox() {
        UUID workOrderId = UUID.randomUUID();
        BudgetApprovedEvent event = new BudgetApprovedEvent(workOrderId);

        publisher.publish(event);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxStore).save(captor.capture());

        OutboxMessage saved = captor.getValue();
        assertThat(saved.id()).isEqualTo(event.messageId());
        assertThat(saved.eventType()).isEqualTo(BudgetApprovedEvent.class.getSimpleName());
        assertThat(saved.aggregateId()).isEqualTo(workOrderId);
        assertThat(saved.occurredAt()).isNotNull();
        assertThat(saved.payload()).contains("\"workOrderId\":\"" + workOrderId + "\"");
        assertThat(meterRegistry.counter("outbox.messages.enqueued").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Não deve publicar evento nulo")
    void shouldIgnoreNullEvent() {
        publisher.publish(null);

        verifyNoInteractions(outboxStore);
    }

    @Test
    @DisplayName("Deve falhar quando não houver transação ativa")
    void shouldFailWithoutActiveTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        BudgetApprovedEvent event = new BudgetApprovedEvent(UUID.randomUUID());

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active transaction");

        verifyNoInteractions(outboxStore);
    }

    @Test
    @DisplayName("Deve falhar ao publicar evento sem eventType")
    void shouldFailWhenEventTypeIsMissing() {
        MessageHeader event = header(UUID.randomUUID(), " ");

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eventType");

        verifyNoInteractions(outboxStore);
    }

    @Test
    @DisplayName("Deve falhar ao publicar evento sem messageId")
    void shouldFailWhenMessageIdIsMissing() {
        MessageHeader event = header(null, "BudgetApprovedEvent");

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("messageId");

        verifyNoInteractions(outboxStore);
    }

    private MessageHeader header(UUID messageId, String eventType) {
        return new MessageHeader() {
            @Override
            public UUID messageId() {
                return messageId;
            }

            @Override
            public UUID workOrderId() {
                return UUID.randomUUID();
            }

            @Override
            public String eventType() {
                return eventType;
            }
        };
    }
}
