package com.mecanicadm.mecanicadm_api.infra.messaging.rabbit;

import com.mecanicadm.mecanicadm_api.core.shared.domain.event.MessageHeader;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetApprovedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RabbitEventPublisherTest {

    private static final String EXCHANGE = "mecanicadm-finance-api-output";
    private static final String EVENT_TYPE_HEADER = "event-type";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitEventPublisher(rabbitTemplate, EXCHANGE, EVENT_TYPE_HEADER);
    }

    @Test
    @DisplayName("Deve publicar evento com routing key vazia e header de eventType")
    void shouldPublishEventWithEventTypeHeader() {
        BudgetApprovedEvent event = new BudgetApprovedEvent(UUID.randomUUID());

        publisher.publish(event);

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(""), eq(event), postProcessor.capture());

        Message message = new Message(new byte[0], new MessageProperties());
        postProcessor.getValue().postProcessMessage(message);

        Object header = message.getMessageProperties().getHeader(EVENT_TYPE_HEADER);
        assertThat(header).isEqualTo(BudgetApprovedEvent.class.getSimpleName());
    }

    @Test
    @DisplayName("Não deve publicar evento nulo")
    void shouldIgnoreNullEvent() {
        publisher.publish(null);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Deve falhar ao publicar evento sem eventType")
    void shouldFailWhenEventTypeIsMissing() {
        MessageHeader event = new MessageHeader() {
            @Override
            public UUID messageId() {
                return UUID.randomUUID();
            }

            @Override
            public UUID workOrderId() {
                return UUID.randomUUID();
            }

            @Override
            public ZonedDateTime ocurredAt() {
                return ZonedDateTime.now();
            }

            @Override
            public String eventType() {
                return " ";
            }
        };

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eventType");

        verifyNoInteractions(rabbitTemplate);
    }
}
