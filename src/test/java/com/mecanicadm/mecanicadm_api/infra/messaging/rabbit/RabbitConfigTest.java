package com.mecanicadm.mecanicadm_api.infra.messaging.rabbit;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    private static final String EXCHANGE_NAME = "mecanicadm-finance-api-output";
    private static final String QUEUE_NAME = "budget";
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
    @DisplayName("Deve declarar a fila budget como durável")
    void shouldDeclareDurableQueue() {
        Queue queue = rabbitConfig.budgetQueue(QUEUE_NAME);

        assertThat(queue.getName()).isEqualTo(QUEUE_NAME);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Deve vincular a fila budget aos eventTypes de entrada por header")
    void shouldBindQueueByEventTypeHeader() {
        Queue queue = rabbitConfig.budgetQueue(QUEUE_NAME);
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
