package com.mecanicadm.mecanicadm_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetQueueListenerTest {

    private static final String EVENT_TYPE_HEADER = "event-type";

    @Mock
    private Jackson2JsonMessageConverter messageConverter;
    @Mock
    private GenerateBudgetIntegrationListener generateBudgetListener;
    @Mock
    private CancelBudgetIntegrationListener cancelBudgetListener;
    @Mock
    private WorkOrderCreatedIntegrationListener workOrderCreatedListener;

    private BudgetQueueListener listener;

    @BeforeEach
    void setUp() {
        listener = new BudgetQueueListener(messageConverter, generateBudgetListener,
                cancelBudgetListener, workOrderCreatedListener, EVENT_TYPE_HEADER);
    }

    @Test
    @DisplayName("Deve direcionar mensagem de geração de orçamento pelo header")
    void shouldDispatchGenerateBudgetByHeader() {
        GenerateBudgetIntegrationCommand command =
                new GenerateBudgetIntegrationCommand(UUID.randomUUID(), BigDecimal.TEN);
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(command);

        listener.onMessage(messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName()));

        verify(generateBudgetListener).onMessage(command);
        verifyNoInteractions(cancelBudgetListener, workOrderCreatedListener);
    }

    @Test
    @DisplayName("Deve direcionar mensagem de cancelamento de orçamento pelo header")
    void shouldDispatchCancelBudgetByHeader() {
        CancelBudgetIntegrationCommand command = new CancelBudgetIntegrationCommand(UUID.randomUUID());
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(command);

        listener.onMessage(messageWithEventType(CancelBudgetIntegrationCommand.class.getSimpleName()));

        verify(cancelBudgetListener).onMessage(command);
        verifyNoInteractions(generateBudgetListener, workOrderCreatedListener);
    }

    @Test
    @DisplayName("Deve direcionar WorkOrderCreatedEvent pelo header")
    void shouldDispatchWorkOrderCreatedByHeader() {
        WorkOrderCreatedEvent event = new WorkOrderCreatedEvent(
                UUID.randomUUID(), BigDecimal.TEN, WorkOrderBudgetStatus.PENDING, "obs",
                "description", null, null, List.of(), List.of(), null, null);
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(event);

        listener.onMessage(messageWithEventType(WorkOrderCreatedEvent.class.getSimpleName()));

        verify(workOrderCreatedListener).onMessage(event);
        verifyNoInteractions(generateBudgetListener, cancelBudgetListener);
    }

    @Test
    @DisplayName("Deve ler header enviado como bytes")
    void shouldReadByteArrayHeader() {
        GenerateBudgetIntegrationCommand command =
                new GenerateBudgetIntegrationCommand(UUID.randomUUID(), BigDecimal.TEN);
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(command);

        MessageProperties properties = new MessageProperties();
        properties.setHeader(EVENT_TYPE_HEADER, GenerateBudgetIntegrationCommand.class.getSimpleName().getBytes(StandardCharsets.UTF_8));
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), properties);

        listener.onMessage(message);

        verify(generateBudgetListener).onMessage(command);
    }

    @Test
    @DisplayName("Não deve direcionar mensagem sem header de eventType")
    void shouldIgnoreMessageWithoutEventTypeHeader() {
        listener.onMessage(messageWithEventType(null));

        verifyNoInteractions(messageConverter, generateBudgetListener,
                cancelBudgetListener, workOrderCreatedListener);
    }

    @Test
    @DisplayName("Não deve direcionar mensagem com eventType desconhecido")
    void shouldIgnoreMessageWithUnknownEventType() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(EVENT_TYPE_HEADER, "finance.unknown.event");

        listener.onMessage(new Message("{}".getBytes(StandardCharsets.UTF_8), properties));

        verifyNoInteractions(messageConverter, generateBudgetListener,
                cancelBudgetListener, workOrderCreatedListener);
    }

    private Message messageWithEventType(String eventType) {
        MessageProperties properties = new MessageProperties();
        if (eventType != null) {
            properties.setHeader(EVENT_TYPE_HEADER, eventType);
        }
        return new Message("{}".getBytes(StandardCharsets.UTF_8), properties);
    }
}
