package com.mecanicadm.mecanicadm_api.core.workorder.domain;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class WorkOrderBudget {

    private UUID workOrderId;

    private BigDecimal totalPrice;

    private WorkOrderBudgetStatus status;

    private String observation;

    private WorkOrderBudgetInfo info = new WorkOrderBudgetInfo();

    protected WorkOrderBudget() {
    }

    private WorkOrderBudget(UUID workOrderId, BigDecimal totalPrice) {
        this.workOrderId = requireNonNull(workOrderId);
        this.totalPrice = requireNonNull(totalPrice);
        this.status = WorkOrderBudgetStatus.PENDING;
        validate();
    }

    private WorkOrderBudget(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation) {
        this.workOrderId = workOrderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.observation = observation;
        validate();
    }

    private WorkOrderBudget(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation,
                            Set<WorkOrderBudgetItem> items) {
        this.workOrderId = workOrderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.observation = observation;
        updateItems(items);
        validate();
    }

    private WorkOrderBudget(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation,
                            WorkOrderBudgetInfo info) {
        this.workOrderId = workOrderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.observation = observation;
        updateInfo(info);
        validate();
    }

    public static WorkOrderBudget create(UUID workOrderId, BigDecimal totalPrice) {
        return new WorkOrderBudget(workOrderId, totalPrice);
    }

    public static WorkOrderBudget restore(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation) {
        return new WorkOrderBudget(workOrderId, totalPrice, status, observation);
    }

    public static WorkOrderBudget restore(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation,
                                          Set<WorkOrderBudgetItem> items) {
        return new WorkOrderBudget(workOrderId, totalPrice, status, observation, items);
    }

    public static WorkOrderBudget restore(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation,
                                          WorkOrderBudgetInfo info) {
        return new WorkOrderBudget(workOrderId, totalPrice, status, observation, info);
    }

    public void updateTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = requireNonNull(totalPrice);
        this.status = WorkOrderBudgetStatus.PENDING;
        this.observation = null;
        validate();
    }

    public void send() {
        this.status = WorkOrderBudgetStatus.WAITING_DECISION;
    }

    public void approve(String note) {
        this.observation = note;
        this.status = WorkOrderBudgetStatus.APPROVED;
    }

    public void reject(String reason, boolean needsRevision) {
        this.observation = requireNonNull(reason);
        this.status = needsRevision ? WorkOrderBudgetStatus.CHANGES_REQUESTED : WorkOrderBudgetStatus.REJECTED;
    }

    public void cancel() {
        this.status = WorkOrderBudgetStatus.CANCELED;
    }

    public void updateItems(Set<WorkOrderBudgetItem> items) {
        this.info.setItems(items);
    }

    public void updateInfo(WorkOrderBudgetInfo info) {
        this.info = info == null ? new WorkOrderBudgetInfo() : info;
    }

    private void validate() {
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BudgetExceptions.BudgetTotalPriceInvalid();
        }
        if (status == null) {
            throw new BudgetExceptions.BudgetStatusInvalid();
        }
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public WorkOrderBudgetStatus getStatus() {
        return status;
    }

    public String getObservation() {
        return observation;
    }

    public Set<WorkOrderBudgetItem> getItems() {
        return info.getItems();
    }

    public String getDescription() {
        return info.getDescription();
    }

    public WorkOrderBudgetInfo getInfo() {
        return info;
    }
}
