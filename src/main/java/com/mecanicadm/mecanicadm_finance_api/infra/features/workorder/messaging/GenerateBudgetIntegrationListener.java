package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderSnapshot;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CreateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CreateWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox.InboundMessageContext;
import com.mecanicadm.mecanicadm_finance_api.shared.exception.DomainExceptionCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GenerateBudgetIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(GenerateBudgetIntegrationListener.class);

    private final CreateWorkOrderBudgetUseCase createWorkOrderBudgetUseCase;
    private final RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    public GenerateBudgetIntegrationListener(CreateWorkOrderBudgetUseCase createWorkOrderBudgetUseCase,
                                             RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase) {
        this.createWorkOrderBudgetUseCase = createWorkOrderBudgetUseCase;
        this.rejeitarFinanceCommandUseCase = rejeitarFinanceCommandUseCase;
    }

    public void onMessage(GenerateBudgetIntegrationCommand command) {
        logger.info("Recebido GenerateBudgetIntegrationCommand para a OS {} (totalPrice={})",
                command.workOrderId(),
                command.workOrderSnapshot() == null ? null : command.workOrderSnapshot().totalPrice());

        try {
            createWorkOrderBudgetUseCase.execute(toCreateCommand(command));
        } catch (DomainExceptionCore e) {
            rejeitarFinanceCommandUseCase.execute(new RejeitarFinanceCommandCommand(
                    command.workOrderId(),
                    GenerateBudgetIntegrationCommand.class.getSimpleName(),
                    e,
                    InboundMessageContext.current()));
            throw e;
        }
    }

    private CreateWorkOrderBudgetCommand toCreateCommand(GenerateBudgetIntegrationCommand command) {
        WorkOrderSnapshot snapshot = command.workOrderSnapshot();
        return new CreateWorkOrderBudgetCommand(
                command.workOrderId(),
                snapshot.totalPrice(),
                snapshot.status(),
                snapshot.observation(),
                snapshot.description(),
                snapshot.client(),
                snapshot.vehicle(),
                snapshot.materialItems(),
                snapshot.laborItems()
        );
    }
}
