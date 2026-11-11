package com.mecanicadm.mecanicadm_finance_api.infra.messaging.rabbit;

import com.mecanicadm.mecanicadm_finance_api.infra.messaging.outbox.OutboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitEventSenderTest {

    private static final String EXCHANGE = "finance-output";
    private static final String EVENT_TYPE_HEADER = "event-type";
    private static final String MESSAGE_ID_HEADER = "message-id";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitEventSender sender;

    @BeforeEach
    void setUp() {
        sender = new RabbitEventSender(rabbitTemplate, EXCHANGE, EVENT_TYPE_HEADER, MESSAGE_ID_HEADER);
    }

    @Test
    @DisplayName("Deve enviar o payload original com os headers esperados")
    void shouldSendMessageWithHeaders() {
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        OutboxMessage message = new OutboxMessage(
                id, "BudgetApprovedEvent", aggregateId, "{\"workOrderId\":\"123\"}", occurredAt);

        sender.send(message);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(EXCHANGE), eq(""), captor.capture());

        Message sent = captor.getValue();
        MessageProperties properties = sent.getMessageProperties();
        assertThat(new String(sent.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"workOrderId\":\"123\"}");
        assertThat(properties.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(properties.getContentEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(properties.getMessageId()).isEqualTo(id.toString());
        assertThat(properties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat((Object) properties.getHeader(EVENT_TYPE_HEADER)).isEqualTo("BudgetApprovedEvent");
        assertThat((Object) properties.getHeader(MESSAGE_ID_HEADER)).isEqualTo(id.toString());
        assertThat(properties.getTimestamp()).isEqualTo(Date.from(occurredAt));
    }

    @Test
    @DisplayName("Não deve incluir o header aggregate-id")
    void shouldNotIncludeAggregateHeader() {
        UUID aggregateId = UUID.randomUUID();
        OutboxMessage message = new OutboxMessage(
                UUID.randomUUID(), "BudgetApprovedEvent", aggregateId, "{}", Instant.now());

        sender.send(message);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(EXCHANGE), eq(""), captor.capture());
        assertThat((Object) captor.getValue().getMessageProperties().getHeader("aggregate-id")).isNull();
    }

    @Test
    @DisplayName("Deve incluir o header message-id mesmo quando não houver agregado")
    void shouldIncludeMessageIdHeaderWhenAggregateIsMissing() {
        UUID id = UUID.randomUUID();
        OutboxMessage message = new OutboxMessage(
                id, "BudgetApprovedEvent", null, "{}", Instant.now());

        sender.send(message);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(EXCHANGE), eq(""), captor.capture());
        assertThat((Object) captor.getValue().getMessageProperties().getHeader(MESSAGE_ID_HEADER))
                .isEqualTo(id.toString());
    }
}
