package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

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

    public GenerateBudgetIntegrationListener(CalculateWorkOrderBudgetUseCase calculateWorkOrderBudgetUseCase) {
        this.calculateWorkOrderBudgetUseCase = calculateWorkOrderBudgetUseCase;
    }

    public void onMessage(GenerateBudgetIntegrationCommand command) {
        logger.info("Recebido GenerateBudgetIntegrationCommand para a OS {} (totalPrice={})",
                command.workOrderId(), command.totalPrice());

        calculateWorkOrderBudgetUseCase.execute(
                new CalculateWorkOrderBudgetCommand(command.workOrderId(), command.totalPrice()));
    }
}
