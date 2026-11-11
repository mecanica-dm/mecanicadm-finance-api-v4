package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.ProcessInboundMessageUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetQueueListenerTest {

    private static final String EVENT_TYPE_HEADER = "event-type";
    private static final String CONSUMER = "budget";

    @Mock
    private Jackson2JsonMessageConverter messageConverter;
    @Mock
    private GenerateBudgetIntegrationListener generateBudgetListener;
    @Mock
    private CancelBudgetIntegrationListener cancelBudgetListener;
    @Mock
    private ProcessInboundMessageUseCase processInboundMessageUseCase;

    private SimpleMeterRegistry meterRegistry;
    private BudgetQueueListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new BudgetQueueListener(messageConverter, generateBudgetListener,
                cancelBudgetListener, processInboundMessageUseCase,
                meterRegistry, CONSUMER, EVENT_TYPE_HEADER);
    }

    @Test
    @DisplayName("Deve direcionar mensagem de geração de orçamento pelo header")
    void shouldDispatchGenerateBudgetByHeader() {
        GenerateBudgetIntegrationCommand command = generateCommand();
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(command);
        executeHandler();

        listener.onMessage(messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName()));

        verify(generateBudgetListener).onMessage(command);
        verifyNoInteractions(cancelBudgetListener);
        assertThat(meterRegistry.counter("inbox.messages.processed").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve direcionar mensagem de cancelamento de orçamento pelo header")
    void shouldDispatchCancelBudgetByHeader() {
        CancelBudgetIntegrationCommand command = new CancelBudgetIntegrationCommand(UUID.randomUUID());
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(command);
        executeHandler();

        listener.onMessage(messageWithEventType(CancelBudgetIntegrationCommand.class.getSimpleName()));

        verify(cancelBudgetListener).onMessage(command);
        verifyNoInteractions(generateBudgetListener);
    }

    @Test
    @DisplayName("Não deve direcionar o WorkOrderCreatedEvent legado")
    void shouldIgnoreLegacyWorkOrderCreatedEvent() {
        listener.onMessage(messageWithEventType("WorkOrderCreatedEvent"));

        verifyNoInteractions(messageConverter, generateBudgetListener,
                cancelBudgetListener, processInboundMessageUseCase);
    }

    @Test
    @DisplayName("Deve ler header enviado como bytes")
    void shouldReadByteArrayHeader() {
        GenerateBudgetIntegrationCommand command = generateCommand();
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(command);
        executeHandler();

        MessageProperties properties = new MessageProperties();
        properties.setHeader(EVENT_TYPE_HEADER,
                GenerateBudgetIntegrationCommand.class.getSimpleName().getBytes(StandardCharsets.UTF_8));
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), properties);

        listener.onMessage(message);

        verify(generateBudgetListener).onMessage(command);
    }

    @Test
    @DisplayName("Não deve direcionar mensagem sem header de eventType")
    void shouldIgnoreMessageWithoutEventTypeHeader() {
        listener.onMessage(messageWithEventType(null));

        verifyNoInteractions(messageConverter, generateBudgetListener,
                cancelBudgetListener, processInboundMessageUseCase);
    }

    @Test
    @DisplayName("Não deve direcionar mensagem com eventType desconhecido")
    void shouldIgnoreMessageWithUnknownEventType() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(EVENT_TYPE_HEADER, "finance.unknown.event");

        listener.onMessage(new Message("{}".getBytes(StandardCharsets.UTF_8), properties));

        verifyNoInteractions(messageConverter, generateBudgetListener,
                cancelBudgetListener, processInboundMessageUseCase);
    }

    @Test
    @DisplayName("Não deve processar mensagem duplicada")
    void shouldIgnoreDuplicatedMessage() {
        when(processInboundMessageUseCase.execute(any(InboxEntry.class), any(Runnable.class)))
                .thenReturn(false);

        listener.onMessage(messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName()));

        verifyNoInteractions(messageConverter, generateBudgetListener,
                cancelBudgetListener);
        assertThat(meterRegistry.counter("inbox.messages.duplicated").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve propagar falha do processamento para o container")
    void shouldPropagateProcessingFailure() {
        when(processInboundMessageUseCase.execute(any(InboxEntry.class), any(Runnable.class)))
                .thenThrow(new IllegalStateException("falha"));

        assertThatThrownBy(() ->
                listener.onMessage(messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("falha");

        assertThat(meterRegistry.counter("inbox.messages.failed").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve usar o messageId do AMQP como chave da inbox")
    void shouldResolveMessageIdFromAmqpHeader() {
        UUID messageId = UUID.randomUUID();
        Message message = messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName());
        message.getMessageProperties().setMessageId(messageId.toString());

        listener.onMessage(message);

        ArgumentCaptor<InboxEntry> captor = ArgumentCaptor.forClass(InboxEntry.class);
        verify(processInboundMessageUseCase).execute(captor.capture(), any(Runnable.class));

        InboxEntry entry = captor.getValue();
        assertThat(entry.messageId()).isEqualTo(messageId);
        assertThat(entry.consumer()).isEqualTo(CONSUMER);
        assertThat(entry.eventType()).isEqualTo(GenerateBudgetIntegrationCommand.class.getSimpleName());
    }

    @Test
    @DisplayName("Deve ignorar o header aggregate-id")
    void shouldIgnoreAggregateIdHeader() {
        Message message = messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName());
        message.getMessageProperties().setHeader("aggregate-id", UUID.randomUUID().toString());

        listener.onMessage(message);

        ArgumentCaptor<InboxEntry> captor = ArgumentCaptor.forClass(InboxEntry.class);
        verify(processInboundMessageUseCase).execute(captor.capture(), any(Runnable.class));
        assertThat(captor.getValue().aggregateId()).isNull();
    }

    @Test
    @DisplayName("Deve gerar chave determinística quando não houver messageId")
    void shouldGenerateDeterministicKeyWhenMessageIdIsMissing() {
        listener.onMessage(messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName()));
        listener.onMessage(messageWithEventType(GenerateBudgetIntegrationCommand.class.getSimpleName()));

        ArgumentCaptor<InboxEntry> captor = ArgumentCaptor.forClass(InboxEntry.class);
        verify(processInboundMessageUseCase, times(2)).execute(captor.capture(), any(Runnable.class));

        assertThat(captor.getAllValues().get(0).messageId())
                .isEqualTo(captor.getAllValues().get(1).messageId());
    }

    private void executeHandler() {
        when(processInboundMessageUseCase.execute(any(InboxEntry.class), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(1, Runnable.class).run();
                    return true;
                });
    }

    private GenerateBudgetIntegrationCommand generateCommand() {
        return new GenerateBudgetIntegrationCommand(
                UUID.randomUUID(),
                new WorkOrderSnapshot(BigDecimal.TEN, null, null, null, null, null, null, null));
    }

    private Message messageWithEventType(String eventType) {
        MessageProperties properties = new MessageProperties();
        if (eventType != null) {
            properties.setHeader(EVENT_TYPE_HEADER, eventType);
        }
        return new Message("{}".getBytes(StandardCharsets.UTF_8), properties);
    }
}
