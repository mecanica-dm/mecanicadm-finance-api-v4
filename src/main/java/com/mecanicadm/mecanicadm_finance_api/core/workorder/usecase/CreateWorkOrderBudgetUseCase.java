package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetItem;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetVehicle;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.BudgetGeneratedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.CreateWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.VoidUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CreateWorkOrderBudgetUseCase implements VoidUseCase<CreateWorkOrderBudgetCommand> {

    private final WorkOrderBudgetGateway gateway;
    private final EventPublisher eventPublisher;

    public CreateWorkOrderBudgetUseCase(WorkOrderBudgetGateway gateway, EventPublisher eventPublisher) {
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(CreateWorkOrderBudgetCommand cmd) {
        Set<WorkOrderBudgetItem> items = buildItems(cmd.materialItems(), cmd.laborItems());
        BigDecimal totalPrice = resolveTotalPrice(cmd.totalPrice(), items);
        WorkOrderBudgetInfo info = new WorkOrderBudgetInfo(
                toClient(cmd.client()),
                toVehicle(cmd.vehicle()),
                cmd.description(),
                items
        );

        WorkOrderBudget budget = WorkOrderBudget.restore(
                cmd.workOrderId(),
                totalPrice,
                cmd.status() == null ? WorkOrderBudgetStatus.PENDING : cmd.status(),
                cmd.observation(),
                info
        );

        gateway.save(budget);
        eventPublisher.publish(new BudgetGeneratedEvent(budget.getWorkOrderId(), budget.getTotalPrice()));
    }

    private BigDecimal resolveTotalPrice(BigDecimal informedTotalPrice, Set<WorkOrderBudgetItem> items) {
        if (informedTotalPrice != null) {
            return informedTotalPrice;
        }
        return items.stream()
                .map(this::itemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal itemTotal(WorkOrderBudgetItem item) {
        BigDecimal price = Objects.requireNonNullElse(item.getPrice(), BigDecimal.ZERO);
        if (WorkOrderBudgetItem.TYPE_MATERIAL.equals(item.getType())) {
            return price.multiply(BigDecimal.valueOf(item.getQuantity()));
        }
        return price;
    }

    private WorkOrderBudgetClient toClient(WorkOrderCreatedEvent.Client client) {
        if (client == null) {
            return null;
        }
        return WorkOrderBudgetClient.restore(
                client.id(),
                client.name(),
                client.email(),
                client.document(),
                client.phone()
        );
    }

    private WorkOrderBudgetVehicle toVehicle(WorkOrderCreatedEvent.Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return WorkOrderBudgetVehicle.restore(
                vehicle.licensePlate(),
                vehicle.model(),
                vehicle.brand(),
                vehicle.modelYear()
        );
    }

    private Set<WorkOrderBudgetItem> buildItems(List<WorkOrderCreatedEvent.MaterialItem> materialItems,
                                                List<WorkOrderCreatedEvent.LaborItem> laborItems) {
        Set<WorkOrderBudgetItem> items = new LinkedHashSet<>();

        valuesOf(materialItems).forEach(item -> items.add(WorkOrderBudgetItem.restore(
                item.id(),
                item.name(),
                item.quantity(),
                item.price(),
                WorkOrderBudgetItem.TYPE_MATERIAL
        )));

        valuesOf(laborItems).forEach(item -> items.add(WorkOrderBudgetItem.restore(
                item.id(),
                item.name(),
                1,
                item.price(),
                WorkOrderBudgetItem.TYPE_LABOR
        )));

        return items;
    }

    private <T> List<T> valuesOf(List<T> items) {
        return Objects.isNull(items) ? List.of() : items;
    }
}
