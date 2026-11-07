package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

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

    public CancelBudgetIntegrationListener(CancelWorkOrderBudgetUseCase cancelWorkOrderBudgetUseCase) {
        this.cancelWorkOrderBudgetUseCase = cancelWorkOrderBudgetUseCase;
    }

    public void onMessage(CancelBudgetIntegrationCommand command) {
        logger.info("Recebido CancelBudgetIntegrationCommand para a OS {}", command.workOrderId());

        cancelWorkOrderBudgetUseCase.execute(
                new CancelWorkOrderBudgetCommand(command.workOrderId()));
    }
}
