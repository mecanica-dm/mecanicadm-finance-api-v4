package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CreateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CreateWorkOrderBudgetCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class WorkOrderCreatedIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderCreatedIntegrationListener.class);

    private final CreateWorkOrderBudgetUseCase createWorkOrderBudgetUseCase;

    public WorkOrderCreatedIntegrationListener(CreateWorkOrderBudgetUseCase createWorkOrderBudgetUseCase) {
        this.createWorkOrderBudgetUseCase = createWorkOrderBudgetUseCase;
    }

    public void onMessage(WorkOrderCreatedEvent event) {
        logger.info("Recebido WorkOrderCreatedEvent para a OS {} (totalPrice={}, materialItems={}, laborItems={})",
                event.workOrderId(), event.totalPrice(),
                sizeOf(event.materialItems()), sizeOf(event.laborItems()));

        createWorkOrderBudgetUseCase.execute(new CreateWorkOrderBudgetCommand(
                event.workOrderId(),
                event.totalPrice(),
                event.status(),
                event.observation(),
                event.description(),
                event.client(),
                event.vehicle(),
                event.materialItems(),
                event.laborItems()
        ));
    }

    private int sizeOf(List<?> items) {
        return Objects.isNull(items) ? 0 : items.size();
    }
}
