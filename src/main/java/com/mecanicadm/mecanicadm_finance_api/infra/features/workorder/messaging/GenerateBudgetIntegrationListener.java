package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CalculateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CalculateWorkOrderBudgetCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GenerateBudgetIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(GenerateBudgetIntegrationListener.class);

    private final CalculateWorkOrderBudgetUseCase calculateWorkOrderBudgetUseCase;
    private final RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    public GenerateBudgetIntegrationListener(CalculateWorkOrderBudgetUseCase calculateWorkOrderBudgetUseCase,
                                             RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase) {
        this.calculateWorkOrderBudgetUseCase = calculateWorkOrderBudgetUseCase;
        this.rejeitarFinanceCommandUseCase = rejeitarFinanceCommandUseCase;
    }

    public void onMessage(GenerateBudgetIntegrationCommand command) {
        logger.info("Recebido GenerateBudgetIntegrationCommand para a OS {} (totalPrice={})",
                command.workOrderId(), command.totalPrice());

        try {
            calculateWorkOrderBudgetUseCase.execute(
                    new CalculateWorkOrderBudgetCommand(command.workOrderId(), command.totalPrice()));
        } catch (RuntimeException e) {
            logger.error("Falha ao processar GenerateBudgetIntegrationCommand para a OS {}: {}",
                    command.workOrderId(), e.getMessage(), e);
            rejeitarFinanceCommandUseCase.execute(new RejeitarFinanceCommandCommand(
                    command.workOrderId(),
                    GenerateBudgetIntegrationCommand.class.getSimpleName(),
                    e));
            throw e;
        }
    }
}
