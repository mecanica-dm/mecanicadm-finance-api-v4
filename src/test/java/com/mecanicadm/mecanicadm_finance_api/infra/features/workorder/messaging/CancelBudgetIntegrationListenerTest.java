package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CancelWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CancelWorkOrderBudgetCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CancelBudgetIntegrationListenerTest {

    @Mock
    private CancelWorkOrderBudgetUseCase cancelWorkOrderBudgetUseCase;
    @Mock
    private RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    private CancelBudgetIntegrationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CancelBudgetIntegrationListener(cancelWorkOrderBudgetUseCase,
                rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve executar o caso de uso com o comando recebido")
    void shouldExecuteUseCaseWithReceivedCommand() {
        UUID workOrderId = UUID.randomUUID();
        CancelBudgetIntegrationCommand command = new CancelBudgetIntegrationCommand(workOrderId);

        listener.onMessage(command);

        verify(cancelWorkOrderBudgetUseCase).execute(new CancelWorkOrderBudgetCommand(workOrderId));
        verifyNoInteractions(rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve publicar evento de rejeição e propagar a exceção do serviço")
    void shouldPublishRejectionAndRethrowWhenServiceFails() {
        UUID workOrderId = UUID.randomUUID();
        CancelBudgetIntegrationCommand command = new CancelBudgetIntegrationCommand(workOrderId);
        BudgetExceptions.BudgetNotFound failure = new BudgetExceptions.BudgetNotFound();
        doThrow(failure).when(cancelWorkOrderBudgetUseCase).execute(any(CancelWorkOrderBudgetCommand.class));

        assertThatThrownBy(() -> listener.onMessage(command))
                .isSameAs(failure);

        verify(rejeitarFinanceCommandUseCase).execute(new RejeitarFinanceCommandCommand(
                workOrderId, CancelBudgetIntegrationCommand.class.getSimpleName(), failure));
    }
}
