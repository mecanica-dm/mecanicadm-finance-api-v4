package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderSnapshot;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CreateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CreateWorkOrderBudgetCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GenerateBudgetIntegrationListenerTest {

    @Mock
    private CreateWorkOrderBudgetUseCase createWorkOrderBudgetUseCase;
    @Mock
    private RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    private GenerateBudgetIntegrationListener listener;

    @BeforeEach
    void setUp() {
        listener = new GenerateBudgetIntegrationListener(createWorkOrderBudgetUseCase,
                rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve criar a WorkOrder com o snapshot recebido no comando")
    void shouldCreateWorkOrderFromReceivedSnapshot() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderSnapshot snapshot = snapshot();
        GenerateBudgetIntegrationCommand command =
                new GenerateBudgetIntegrationCommand(workOrderId, snapshot);

        listener.onMessage(command);

        verify(createWorkOrderBudgetUseCase).execute(new CreateWorkOrderBudgetCommand(
                workOrderId,
                snapshot.totalPrice(),
                snapshot.status(),
                snapshot.observation(),
                snapshot.description(),
                snapshot.client(),
                snapshot.vehicle(),
                snapshot.materialItems(),
                snapshot.laborItems()));
        verifyNoInteractions(rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve publicar evento de rejeição e propagar a exceção do serviço")
    void shouldPublishRejectionAndRethrowWhenServiceFails() {
        UUID workOrderId = UUID.randomUUID();
        GenerateBudgetIntegrationCommand command =
                new GenerateBudgetIntegrationCommand(workOrderId, snapshot());
        BudgetExceptions.BudgetNotFound failure = new BudgetExceptions.BudgetNotFound();
        doThrow(failure).when(createWorkOrderBudgetUseCase).execute(any(CreateWorkOrderBudgetCommand.class));

        assertThatThrownBy(() -> listener.onMessage(command))
                .isSameAs(failure);

        verify(rejeitarFinanceCommandUseCase).execute(new RejeitarFinanceCommandCommand(
                workOrderId, GenerateBudgetIntegrationCommand.class.getSimpleName(), failure));
    }

    private WorkOrderSnapshot snapshot() {
        return new WorkOrderSnapshot(
                BigDecimal.TEN,
                WorkOrderBudgetStatus.PENDING,
                "observacao",
                "descricao",
                null,
                null,
                List.of(),
                List.of());
    }
}
