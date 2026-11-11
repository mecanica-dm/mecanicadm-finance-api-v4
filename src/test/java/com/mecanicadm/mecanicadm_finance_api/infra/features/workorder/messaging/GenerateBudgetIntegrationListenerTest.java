package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CalculateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CalculateWorkOrderBudgetCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GenerateBudgetIntegrationListenerTest {

    @Mock
    private CalculateWorkOrderBudgetUseCase calculateWorkOrderBudgetUseCase;
    @Mock
    private RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    private GenerateBudgetIntegrationListener listener;

    @BeforeEach
    void setUp() {
        listener = new GenerateBudgetIntegrationListener(calculateWorkOrderBudgetUseCase,
                rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve executar o caso de uso com o comando recebido")
    void shouldExecuteUseCaseWithReceivedCommand() {
        UUID workOrderId = UUID.randomUUID();
        GenerateBudgetIntegrationCommand command =
                new GenerateBudgetIntegrationCommand(workOrderId, BigDecimal.TEN);

        listener.onMessage(command);

        verify(calculateWorkOrderBudgetUseCase).execute(
                new CalculateWorkOrderBudgetCommand(workOrderId, BigDecimal.TEN));
        verifyNoInteractions(rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve publicar evento de rejeição e propagar a exceção do serviço")
    void shouldPublishRejectionAndRethrowWhenServiceFails() {
        UUID workOrderId = UUID.randomUUID();
        GenerateBudgetIntegrationCommand command =
                new GenerateBudgetIntegrationCommand(workOrderId, BigDecimal.TEN);
        BudgetExceptions.BudgetNotFound failure = new BudgetExceptions.BudgetNotFound();
        doThrow(failure).when(calculateWorkOrderBudgetUseCase).execute(any(CalculateWorkOrderBudgetCommand.class));

        assertThatThrownBy(() -> listener.onMessage(command))
                .isSameAs(failure);

        verify(rejeitarFinanceCommandUseCase).execute(new RejeitarFinanceCommandCommand(
                workOrderId, GenerateBudgetIntegrationCommand.class.getSimpleName(), failure));
    }
}
