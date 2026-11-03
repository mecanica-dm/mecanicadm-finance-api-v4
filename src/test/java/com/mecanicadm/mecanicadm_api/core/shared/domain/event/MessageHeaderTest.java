package com.mecanicadm.mecanicadm_api.core.shared.domain.event;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetApprovedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetCanceledEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetChangesRequestedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetExpiredEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetGeneratedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetRejectedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetSentForDecisionEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetSentForDecisionFailedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetUpdatedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageHeaderTest {

    private static final UUID WORK_ORDER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Deve preencher os dados de cabeçalho de todos os eventos de saída")
    void shouldFillHeaderDataForAllOutputEvents() {
        List<MessageHeader> events = List.of(
                new BudgetApprovedEvent(WORK_ORDER_ID),
                new BudgetCanceledEvent(WORK_ORDER_ID),
                new BudgetChangesRequestedEvent(WORK_ORDER_ID, "obs"),
                new BudgetExpiredEvent(WORK_ORDER_ID),
                new BudgetGeneratedEvent(WORK_ORDER_ID, BigDecimal.TEN),
                new BudgetRejectedEvent(WORK_ORDER_ID, "obs"),
                new BudgetSentForDecisionEvent(WORK_ORDER_ID),
                new BudgetSentForDecisionFailedEvent(WORK_ORDER_ID),
                new BudgetUpdatedEvent(WORK_ORDER_ID, BigDecimal.TEN, "obs"));

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        events.forEach(event -> {
            assertThat(event.messageId()).isNotNull();
            assertThat(event.workOrderId()).isEqualTo(WORK_ORDER_ID);
            assertThat(event.ocurredAt()).isNotNull().isBeforeOrEqualTo(now);
            assertThat(event.eventType()).isEqualTo(event.getClass().getSimpleName());
        });
    }

    @Test
    @DisplayName("Deve gerar cabeçalho padrão no WorkOrderCreatedEvent quando não informado")
    void shouldGenerateDefaultHeaderForWorkOrderCreatedEvent() {
        WorkOrderCreatedEvent event = new WorkOrderCreatedEvent(
                WORK_ORDER_ID, BigDecimal.TEN, WorkOrderBudgetStatus.PENDING, "obs",
                "description", null, null, List.of(), List.of(), null, null);

        assertThat(event.messageId()).isNotNull();
        assertThat(event.ocurredAt()).isNotNull();
        assertThat(event.workOrderId()).isEqualTo(WORK_ORDER_ID);
        assertThat(event.eventType()).isEqualTo(WorkOrderCreatedEvent.class.getSimpleName());
    }

    @Test
    @DisplayName("Deve respeitar messageId e ocurredAt informados no WorkOrderCreatedEvent")
    void shouldKeepInformedHeaderDataForWorkOrderCreatedEvent() {
        UUID messageId = UUID.randomUUID();
        ZonedDateTime occurredAt = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(5);

        WorkOrderCreatedEvent event = new WorkOrderCreatedEvent(
                WORK_ORDER_ID, BigDecimal.TEN, WorkOrderBudgetStatus.APPROVED, "obs",
                "description", null, null, List.of(), List.of(), messageId, occurredAt);

        assertThat(event.messageId()).isEqualTo(messageId);
        assertThat(event.ocurredAt()).isEqualTo(occurredAt);
    }
}
