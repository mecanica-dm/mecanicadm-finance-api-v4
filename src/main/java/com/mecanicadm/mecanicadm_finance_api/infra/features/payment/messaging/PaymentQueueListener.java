package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.GeneratePaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.ProcessInboundMessageUseCase;
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
public class PaymentQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentQueueListener.class);

    private final Jackson2JsonMessageConverter messageConverter;
    private final GeneratePaymentIntegrationListener generatePaymentListener;
    private final ProcessInboundMessageUseCase processInboundMessageUseCase;
    private final MeterRegistry meterRegistry;

    private final String consumer;
    private final String eventTypeHeader;

    public PaymentQueueListener(Jackson2JsonMessageConverter messageConverter,
                                GeneratePaymentIntegrationListener generatePaymentListener,
                                ProcessInboundMessageUseCase processInboundMessageUseCase,
                                MeterRegistry meterRegistry,
                                @Value("${app.rabbitmq.queues.payment:payment}") String consumer,
                                @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader) {
        this.messageConverter = messageConverter;
        this.generatePaymentListener = generatePaymentListener;
        this.processInboundMessageUseCase = processInboundMessageUseCase;
        this.meterRegistry = meterRegistry;
        this.consumer = consumer;
        this.eventTypeHeader = eventTypeHeader;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.payment}")
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
                null,
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
        if (GeneratePaymentIntegrationCommand.class.getSimpleName().equals(eventType)) {
            generatePaymentListener.onMessage(convert(message, GeneratePaymentIntegrationCommand.class));
        }
    }

    private boolean isSupported(String eventType) {
        return GeneratePaymentIntegrationCommand.class.getSimpleName().equals(eventType);
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
