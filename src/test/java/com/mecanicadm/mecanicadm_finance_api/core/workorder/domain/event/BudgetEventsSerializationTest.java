package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.FinanceCommandRejectedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetEventsSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final UUID workOrderId = UUID.randomUUID();

    @Test
    @DisplayName("Cada evento de orcamento deve expor o eventType com o nome da classe, um messageId proprio e o workOrderId")
    void shouldExposeHeaderFieldsOfEveryEvent() {
        List<MessageHeader> events = List.of(
                new BudgetGeneratedEvent(workOrderId, BigDecimal.TEN),
                new BudgetUpdatedEvent(workOrderId, BigDecimal.ONE, "obs"),
                new BudgetSentForDecisionEvent(workOrderId),
                new BudgetSentForDecisionFailedEvent(workOrderId),
                new BudgetApprovedEvent(workOrderId),
                new BudgetRejectedEvent(workOrderId, "caro"),
                new BudgetChangesRequestedEvent(workOrderId, "trocar item"),
                new BudgetCanceledEvent(workOrderId),
                new BudgetExpiredEvent(workOrderId),
                new FinanceCommandRejectedEvent(workOrderId, "GenerateBudgetIntegrationCommand", "BudgetNotFound", "nao achou"));

        events.forEach(event -> {
            assertThat(event.eventType()).isEqualTo(event.getClass().getSimpleName());
            assertThat(event.workOrderId()).isEqualTo(workOrderId);
            assertThat(event.messageId()).isNotNull();
        });
        assertThat(events.stream().map(MessageHeader::messageId).distinct()).hasSize(events.size());
    }

    @Test
    @DisplayName("Deve serializar o evento com os campos que a saga le")
    void shouldSerializeFieldsTheSagaReads() throws Exception {
        JsonNode generated = mapper.valueToTree(new BudgetGeneratedEvent(workOrderId, new BigDecimal("720.00")));
        JsonNode changes = mapper.valueToTree(new BudgetChangesRequestedEvent(workOrderId, "trocar item"));
        JsonNode rejected = mapper.valueToTree(new FinanceCommandRejectedEvent(workOrderId, "CancelBudgetIntegrationCommand", "BudgetNotFound", "x"));

        assertThat(generated.get("workOrderId").asText()).isEqualTo(workOrderId.toString());
        assertThat(generated.get("totalPrice").decimalValue()).isEqualByComparingTo("720.00");
        assertThat(changes.get("observation").asText()).isEqualTo("trocar item");
        assertThat(rejected.get("eventType").asText()).isEqualTo("CancelBudgetIntegrationCommand");
        assertThat(rejected.get("exception").asText()).isEqualTo("BudgetNotFound");
        assertThat(rejected.get("message").asText()).isEqualTo("x");
    }

    @Test
    @DisplayName("Deve desserializar o evento pelos setters, preservando o workOrderId")
    void shouldDeserializeEvents() throws Exception {
        String json = "{\"workOrderId\":\"" + workOrderId + "\",\"observation\":\"obs\",\"totalPrice\":5}";

        assertThat(mapper.readValue(json, BudgetRejectedEvent.class).getObservation()).isEqualTo("obs");
        assertThat(mapper.readValue(json, BudgetUpdatedEvent.class).getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(mapper.readValue(json, BudgetGeneratedEvent.class).getTotalPrice()).isEqualByComparingTo("5");
        assertThat(mapper.readValue(json, BudgetChangesRequestedEvent.class).getObservation()).isEqualTo("obs");
    }
}
