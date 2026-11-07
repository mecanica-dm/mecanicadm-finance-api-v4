package com.mecanicadm.mecanicadm_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_api.core.shared.usecase.ProcessInboundMessageUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class BudgetQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(BudgetQueueListener.class);

    private final Jackson2JsonMessageConverter messageConverter;
    private final GenerateBudgetIntegrationListener generateBudgetListener;
    private final CancelBudgetIntegrationListener cancelBudgetListener;
    private final WorkOrderCreatedIntegrationListener workOrderCreatedListener;
    private final ProcessInboundMessageUseCase processInboundMessageUseCase;
    private final MeterRegistry meterRegistry;

    private final String consumer;
    private final String eventTypeHeader;

    public BudgetQueueListener(Jackson2JsonMessageConverter messageConverter,
                               GenerateBudgetIntegrationListener generateBudgetListener,
                               CancelBudgetIntegrationListener cancelBudgetListener,
                               WorkOrderCreatedIntegrationListener workOrderCreatedListener,
                               ProcessInboundMessageUseCase processInboundMessageUseCase,
                               MeterRegistry meterRegistry,
                               @Value("${app.rabbitmq.queues.budget:budget}") String consumer,
                               @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader) {
        this.messageConverter = messageConverter;
        this.generateBudgetListener = generateBudgetListener;
        this.cancelBudgetListener = cancelBudgetListener;
        this.workOrderCreatedListener = workOrderCreatedListener;
        this.processInboundMessageUseCase = processInboundMessageUseCase;
        this.meterRegistry = meterRegistry;
        this.consumer = consumer;
        this.eventTypeHeader = eventTypeHeader;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.budget}")
    public void onMessage(Message message) {
        String eventType = resolveEventType(message);

        if (!isSupported(eventType)) {
            logger.warn("Nenhum listener configurado para o eventType {} na fila {}; mensagem ignorada",
                    eventType, consumer);
            return;
        }

        UUID messageId = resolveMessageId(message, eventType);
        InboxEntry entry = new InboxEntry(
                messageId,
                consumer,
                eventType,
                resolveAggregateId(message),
                Instant.now()
        );

        logger.info("Recebida mensagem na fila {} (eventType={}, messageId={})",
                consumer, eventType, messageId);

        try {
            boolean processed = processInboundMessageUseCase.execute(
                    entry, () -> dispatch(message, eventType));

            if (processed) {
                meterRegistry.counter("inbox.messages.processed").increment();
            } else {
                meterRegistry.counter("inbox.messages.duplicated").increment();
                logger.info("Mensagem duplicada ignorada (messageId={}, eventType={}, consumer={})",
                        messageId, eventType, consumer);
            }
        } catch (RuntimeException e) {
            meterRegistry.counter("inbox.messages.failed").increment();
            logger.error("Erro ao processar mensagem (messageId={}, eventType={}, consumer={}): {}",
                    messageId, eventType, consumer, e.getMessage(), e);
            throw e;
        }
    }

    private void dispatch(Message message, String eventType) {
        if (GenerateBudgetIntegrationCommand.class.getSimpleName().equals(eventType)) {
            generateBudgetListener.onMessage(convert(message, GenerateBudgetIntegrationCommand.class));
        } else if (CancelBudgetIntegrationCommand.class.getSimpleName().equals(eventType)) {
            cancelBudgetListener.onMessage(convert(message, CancelBudgetIntegrationCommand.class));
        } else if (WorkOrderCreatedEvent.class.getSimpleName().equals(eventType)) {
            workOrderCreatedListener.onMessage(convert(message, WorkOrderCreatedEvent.class));
        }
    }

    private boolean isSupported(String eventType) {
        return GenerateBudgetIntegrationCommand.class.getSimpleName().equals(eventType)
                || CancelBudgetIntegrationCommand.class.getSimpleName().equals(eventType)
                || WorkOrderCreatedEvent.class.getSimpleName().equals(eventType);
    }

    private UUID resolveMessageId(Message message, String eventType) {
        String headerId = message.getMessageProperties().getMessageId();

        if (headerId != null && !headerId.isBlank()) {
            try {
                return UUID.fromString(headerId);
            } catch (IllegalArgumentException ignored) {
                return UUID.nameUUIDFromBytes(headerId.getBytes(StandardCharsets.UTF_8));
            }
        }

        logger.warn("Mensagem sem messageId na fila {} (eventType={}); usando chave determinística",
                consumer, eventType);
        return UUID.nameUUIDFromBytes(messageKey(eventType, message.getBody()));
    }

    private byte[] messageKey(String eventType, byte[] body) {
        byte[] prefix = (eventType + ":").getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[prefix.length + body.length];
        System.arraycopy(prefix, 0, key, 0, prefix.length);
        System.arraycopy(body, 0, key, prefix.length, body.length);
        return key;
    }

    private UUID resolveAggregateId(Message message) {
        String aggregateId = resolveHeader(message, "aggregate-id");
        if (aggregateId == null || aggregateId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(aggregateId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveEventType(Message message) {
        return resolveHeader(message, eventTypeHeader);
    }

    private String resolveHeader(Message message, String headerName) {
        Object header = message.getMessageProperties().getHeader(headerName);
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
