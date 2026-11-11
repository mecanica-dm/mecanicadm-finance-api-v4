package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CancelWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CancelWorkOrderBudgetCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CancelBudgetIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(CancelBudgetIntegrationListener.class);

    private final CancelWorkOrderBudgetUseCase cancelWorkOrderBudgetUseCase;
    private final RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    public CancelBudgetIntegrationListener(CancelWorkOrderBudgetUseCase cancelWorkOrderBudgetUseCase,
                                           RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase) {
        this.cancelWorkOrderBudgetUseCase = cancelWorkOrderBudgetUseCase;
        this.rejeitarFinanceCommandUseCase = rejeitarFinanceCommandUseCase;
    }

    public void onMessage(CancelBudgetIntegrationCommand command) {
        logger.info("Recebido CancelBudgetIntegrationCommand para a OS {}", command.workOrderId());

        try {
            cancelWorkOrderBudgetUseCase.execute(
                    new CancelWorkOrderBudgetCommand(command.workOrderId()));
        } catch (RuntimeException e) {
            logger.error("Falha ao processar CancelBudgetIntegrationCommand para a OS {}: {}",
                    command.workOrderId(), e.getMessage(), e);
            rejeitarFinanceCommandUseCase.execute(new RejeitarFinanceCommandCommand(
                    command.workOrderId(),
                    CancelBudgetIntegrationCommand.class.getSimpleName(),
                    e));
            throw e;
        }
    }
}
