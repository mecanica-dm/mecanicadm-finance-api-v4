package com.mecanicadm.mecanicadm_finance_api.core.shared.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.FinanceCommandRejectedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RejeitarFinanceCommandServiceTest {

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private RejeitarFinanceCommandService service;

    @Test
    @DisplayName("Deve publicar FinanceCommandRejectedEvent com os dados do comando rejeitado")
    void shouldPublishRejectedEventWithCommandDetails() {
        UUID workOrderId = UUID.randomUUID();
        RuntimeException exception = new IllegalStateException("falha ao processar");

        service.execute(new RejeitarFinanceCommandCommand(
                workOrderId, "GenerateBudgetIntegrationCommand", exception));

        ArgumentCaptor<FinanceCommandRejectedEvent> captor = ArgumentCaptor.forClass(FinanceCommandRejectedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        FinanceCommandRejectedEvent event = captor.getValue();
        assertThat(event.getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(event.getEventType()).isEqualTo("GenerateBudgetIntegrationCommand");
        assertThat(event.getException()).isEqualTo("IllegalStateException");
        assertThat(event.getMessage()).isEqualTo("falha ao processar");
    }

    @Test
    @DisplayName("Deve manter o eventType do contrato como nome simples da classe")
    void shouldKeepContractEventTypeAsClassName() {
        service.execute(new RejeitarFinanceCommandCommand(
                UUID.randomUUID(), "CancelBudgetIntegrationCommand", new RuntimeException("erro")));

        ArgumentCaptor<FinanceCommandRejectedEvent> captor = ArgumentCaptor.forClass(FinanceCommandRejectedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        assertThat(captor.getValue().eventType()).isEqualTo("FinanceCommandRejectedEvent");
        assertThat(captor.getValue().messageId()).isNotNull();
    }

    @Test
    @DisplayName("Deve derivar o id do evento do id da mensagem recebida, para a mesma mensagem gerar sempre o mesmo id")
    void shouldDeriveEventIdFromInboundMessageId() {
        UUID inbound = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();

        service.execute(new RejeitarFinanceCommandCommand(workOrderId, "CancelBudgetIntegrationCommand", new RuntimeException("a"), inbound));
        service.execute(new RejeitarFinanceCommandCommand(workOrderId, "CancelBudgetIntegrationCommand", new RuntimeException("b"), inbound));
        service.execute(new RejeitarFinanceCommandCommand(workOrderId, "CancelBudgetIntegrationCommand", new RuntimeException("c"), UUID.randomUUID()));

        ArgumentCaptor<FinanceCommandRejectedEvent> captor = ArgumentCaptor.forClass(FinanceCommandRejectedEvent.class);
        verify(eventPublisher, times(3)).publish(captor.capture());
        assertThat(captor.getAllValues().get(0).messageId()).isEqualTo(captor.getAllValues().get(1).messageId());
        assertThat(captor.getAllValues().get(0).messageId()).isNotEqualTo(captor.getAllValues().get(2).messageId());
    }
}
