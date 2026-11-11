package com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetApprovedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetCanceledEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetChangesRequestedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetExpiredEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetGeneratedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetRejectedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetSentForDecisionEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetSentForDecisionFailedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

        events.forEach(event -> {
            assertThat(event.messageId()).isNotNull();
            assertThat(event.workOrderId()).isEqualTo(WORK_ORDER_ID);
            assertThat(event.eventType()).isEqualTo(event.getClass().getSimpleName());
        });
    }
}
