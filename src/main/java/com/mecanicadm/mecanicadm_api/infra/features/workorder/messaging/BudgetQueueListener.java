package com.mecanicadm.mecanicadm_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class BudgetQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(BudgetQueueListener.class);

    private final Jackson2JsonMessageConverter messageConverter;
    private final GenerateBudgetIntegrationListener generateBudgetListener;
    private final CancelBudgetIntegrationListener cancelBudgetListener;
    private final WorkOrderCreatedIntegrationListener workOrderCreatedListener;

    private final String eventTypeHeader;

    public BudgetQueueListener(Jackson2JsonMessageConverter messageConverter,
                               GenerateBudgetIntegrationListener generateBudgetListener,
                               CancelBudgetIntegrationListener cancelBudgetListener,
                               WorkOrderCreatedIntegrationListener workOrderCreatedListener,
                               @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader) {
        this.messageConverter = messageConverter;
        this.generateBudgetListener = generateBudgetListener;
        this.cancelBudgetListener = cancelBudgetListener;
        this.workOrderCreatedListener = workOrderCreatedListener;
        this.eventTypeHeader = eventTypeHeader;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.budget}")
    public void onMessage(Message message) {
        String eventType = resolveEventType(message);
        logger.info("Recebida mensagem na fila budget (eventType={})", eventType);

        if (GenerateBudgetIntegrationCommand.class.getSimpleName().equals(eventType)) {
            generateBudgetListener.onMessage(convert(message, GenerateBudgetIntegrationCommand.class));
        } else if (CancelBudgetIntegrationCommand.class.getSimpleName().equals(eventType)) {
            cancelBudgetListener.onMessage(convert(message, CancelBudgetIntegrationCommand.class));
        } else if (WorkOrderCreatedEvent.class.getSimpleName().equals(eventType)) {
            workOrderCreatedListener.onMessage(convert(message, WorkOrderCreatedEvent.class));
        } else {
            logger.warn("Nenhum listener configurado para o eventType {} na fila budget; mensagem ignorada",
                    eventType);
        }
    }

    private String resolveEventType(Message message) {
        Object header = message.getMessageProperties().getHeader(eventTypeHeader);
        if (header instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return header != null ? header.toString() : null;
    }

    private <T> T convert(Message message, Class<T> type) {
        message.getMessageProperties().setInferredArgumentType(type);
        return type.cast(messageConverter.fromMessage(message));
    }
}
