package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetExpiredEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetExpiryGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetExpiryGateway.ExpiredBudget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireWorkOrderBudgetsUseCaseTest {

    private final BudgetExpiryGateway gateway = mock(BudgetExpiryGateway.class);
    private final EventPublisher publisher = mock(EventPublisher.class);
    private final ExpireWorkOrderBudgetsUseCase useCase = new ExpireWorkOrderBudgetsUseCase(gateway, publisher);

    @Test
    @DisplayName("Deve publicar o BudgetExpiredEvent de cada orcamento vencido e marcar o token como avisado")
    void shouldPublishExpiredEventAndMarkNotified() {
        UUID workOrderA = UUID.randomUUID();
        UUID workOrderB = UUID.randomUUID();
        UUID tokenA = UUID.randomUUID();
        UUID tokenB = UUID.randomUUID();
        when(gateway.claimExpired(any(), anyInt())).thenReturn(List.of(new ExpiredBudget(tokenA, workOrderA), new ExpiredBudget(tokenB, workOrderB)));

        int notified = useCase.execute(50);

        assertThat(notified).isEqualTo(2);
        ArgumentCaptor<MessageHeader> events = ArgumentCaptor.forClass(MessageHeader.class);
        verify(publisher, times(2)).publish(events.capture());
        assertThat(events.getAllValues()).allMatch(e -> e instanceof BudgetExpiredEvent);
        assertThat(events.getAllValues().stream().map(MessageHeader::workOrderId)).containsExactly(workOrderA, workOrderB);
        verify(gateway).markExpiryNotified(tokenA);
        verify(gateway).markExpiryNotified(tokenB);
    }

    @Test
    @DisplayName("Deve usar o prazo de 24 horas do token e o tamanho do lote informado")
    void shouldUseTokenValidityAndBatchSize() {
        when(gateway.claimExpired(any(), anyInt())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);

        int notified = useCase.execute(7);

        assertThat(notified).isZero();
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(gateway).claimExpired(cutoff.capture(), org.mockito.ArgumentMatchers.eq(7));
        assertThat(cutoff.getValue()).isBetween(before.minusSeconds(5), before.plusSeconds(5));
        verify(publisher, never()).publish(any());
    }
}
