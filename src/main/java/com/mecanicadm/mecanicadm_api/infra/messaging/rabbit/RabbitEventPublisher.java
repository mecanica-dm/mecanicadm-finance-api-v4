package com.mecanicadm.mecanicadm_api.infra.messaging.rabbit;

import com.mecanicadm.mecanicadm_api.core.shared.domain.event.MessageHeader;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String eventTypeHeader;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate,
                                @Value("${app.rabbitmq.exchange.output:mecanicadm-finance-api-output}") String exchange,
                                @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.eventTypeHeader = eventTypeHeader;
    }

    @Override
    public void publish(MessageHeader event) {
        if (event == null) {
            return;
        }
        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new IllegalStateException("Event without eventType: " + event.getClass().getName());
        }
        rabbitTemplate.convertAndSend(exchange, "", event, message -> {
            message.getMessageProperties().setHeader(eventTypeHeader, event.eventType());
            return message;
        });
    }
}
