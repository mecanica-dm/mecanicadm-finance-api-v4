package com.mecanicadm.mecanicadm_api.infra.messaging.rabbit;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    private static final String EXCHANGE_NAME = "finance-output";
    private static final String DEAD_LETTER_EXCHANGE = "finance-dlx";
    private static final String QUEUE_NAME = "budget";
    private static final String DEAD_LETTER_QUEUE = "budget.dlq";
    private static final String EVENT_TYPE_HEADER = "event-type";

    private final RabbitConfig rabbitConfig = new RabbitConfig();

    @Test
    @DisplayName("Deve declarar a exchange de saída como headers, durável e não autodeletável")
    void shouldDeclareHeadersExchange() {
        HeadersExchange exchange = rabbitConfig.financeOutputExchange(EXCHANGE_NAME);

        assertThat(exchange.getName()).isEqualTo(EXCHANGE_NAME);
        assertThat(exchange.getType()).isEqualTo("headers");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    @DisplayName("Deve declarar a exchange de dead letter como direct e durável")
    void shouldDeclareDeadLetterExchange() {
        DirectExchange exchange = rabbitConfig.financeDeadLetterExchange(DEAD_LETTER_EXCHANGE);

        assertThat(exchange.getName()).isEqualTo(DEAD_LETTER_EXCHANGE);
        assertThat(exchange.getType()).isEqualTo("direct");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    @DisplayName("Deve declarar a fila budget como durável e com dead letter")
    void shouldDeclareDurableQueueWithDeadLetter() {
        Queue queue = rabbitConfig.budgetQueue(QUEUE_NAME, DEAD_LETTER_EXCHANGE);

        assertThat(queue.getName()).isEqualTo(QUEUE_NAME);
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", QUEUE_NAME);
    }

    @Test
    @DisplayName("Deve declarar a fila de dead letter como durável")
    void shouldDeclareDeadLetterQueue() {
        Queue queue = rabbitConfig.budgetDeadLetterQueue(DEAD_LETTER_QUEUE);

        assertThat(queue.getName()).isEqualTo(DEAD_LETTER_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Deve vincular a fila de dead letter à exchange de dead letter")
    void shouldBindDeadLetterQueue() {
        Queue queue = rabbitConfig.budgetDeadLetterQueue(DEAD_LETTER_QUEUE);
        DirectExchange exchange = rabbitConfig.financeDeadLetterExchange(DEAD_LETTER_EXCHANGE);

        Binding binding = rabbitConfig.budgetDeadLetterBinding(queue, exchange, QUEUE_NAME);

        assertThat(binding.getExchange()).isEqualTo(DEAD_LETTER_EXCHANGE);
        assertThat(binding.getDestination()).isEqualTo(DEAD_LETTER_QUEUE);
        assertThat(binding.getRoutingKey()).isEqualTo(QUEUE_NAME);
    }

    @Test
    @DisplayName("Deve vincular a fila budget aos eventTypes de entrada por header")
    void shouldBindQueueByEventTypeHeader() {
        Queue queue = rabbitConfig.budgetQueue(QUEUE_NAME, DEAD_LETTER_EXCHANGE);
        HeadersExchange exchange = rabbitConfig.financeOutputExchange(EXCHANGE_NAME);

        assertBinding(rabbitConfig.generateBudgetBinding(queue, exchange, EVENT_TYPE_HEADER),
                GenerateBudgetIntegrationCommand.class.getSimpleName());
        assertBinding(rabbitConfig.cancelBudgetBinding(queue, exchange, EVENT_TYPE_HEADER),
                CancelBudgetIntegrationCommand.class.getSimpleName());
        assertBinding(rabbitConfig.workOrderCreatedBinding(queue, exchange, EVENT_TYPE_HEADER),
                WorkOrderCreatedEvent.class.getSimpleName());
    }

    private void assertBinding(Binding binding, String eventType) {
        assertThat(binding.getExchange()).isEqualTo(EXCHANGE_NAME);
        assertThat(binding.getDestination()).isEqualTo(QUEUE_NAME);
        assertThat(binding.getRoutingKey()).isNullOrEmpty();
        assertThat(binding.getArguments())
                .containsEntry("x-match", "all")
                .containsEntry(EVENT_TYPE_HEADER, eventType);
    }
}
