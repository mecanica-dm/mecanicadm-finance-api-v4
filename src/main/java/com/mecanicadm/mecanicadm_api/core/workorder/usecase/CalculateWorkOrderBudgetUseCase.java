package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudgetItem;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetGeneratedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.CalculateWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_api.shared.usecase.UseCase;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CalculateWorkOrderBudgetUseCase implements UseCase<CalculateWorkOrderBudgetCommand, UUID> {

    private final WorkOrderBudgetGateway gateway;
    private final EventPublisher eventPublisher;

    public CalculateWorkOrderBudgetUseCase(WorkOrderBudgetGateway gateway, EventPublisher eventPublisher) {
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID execute(CalculateWorkOrderBudgetCommand cmd) {
        WorkOrderBudget budget = gateway.findById(cmd.workOrderId()).orElse(null);

        if (budget == null) {
            if (cmd.totalPrice() == null) {
                throw new BudgetExceptions.BudgetNotFound();
            }
            budget = WorkOrderBudget.create(cmd.workOrderId(), cmd.totalPrice());
        } else {
            budget.updateTotalPrice(resolveTotalPrice(budget, cmd.totalPrice()));
        }

        gateway.save(budget);
        eventPublisher.publish(new BudgetGeneratedEvent(budget.getWorkOrderId(), budget.getTotalPrice()));

        return budget.getWorkOrderId();
    }

    private BigDecimal resolveTotalPrice(WorkOrderBudget budget, BigDecimal informedTotalPrice) {
        if (informedTotalPrice != null) {
            return informedTotalPrice;
        }
        return calculateTotalFromItems(budget.getItems());
    }

    private BigDecimal calculateTotalFromItems(Set<WorkOrderBudgetItem> items) {
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
}
