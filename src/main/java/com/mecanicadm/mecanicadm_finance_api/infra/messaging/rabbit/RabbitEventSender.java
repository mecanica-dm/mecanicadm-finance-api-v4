package com.mecanicadm.mecanicadm_finance_api.infra.messaging.rabbit;

import com.mecanicadm.mecanicadm_finance_api.infra.messaging.outbox.OutboxMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class RabbitEventSender {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String eventTypeHeader;
    private final String messageIdHeader;

    public RabbitEventSender(RabbitTemplate rabbitTemplate,
                             @Value("${app.rabbitmq.exchange.output:finance-output}") String exchange,
                             @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader,
                             @Value("${app.rabbitmq.headers.message-id:message-id}") String messageIdHeader) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.eventTypeHeader = eventTypeHeader;
        this.messageIdHeader = messageIdHeader;
    }

    public void send(OutboxMessage message) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(message.id().toString());
        properties.setHeader(messageIdHeader, message.id().toString());
        properties.setHeader(eventTypeHeader, message.eventType());
        properties.setTimestamp(Date.from(message.occurredAt()));

        Message amqpMessage = new Message(
                message.payload().getBytes(StandardCharsets.UTF_8),
                properties);

        rabbitTemplate.send(exchange, "", amqpMessage);
    }
}
