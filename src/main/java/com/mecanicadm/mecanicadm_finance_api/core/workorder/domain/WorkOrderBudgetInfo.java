package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain;

import java.util.HashSet;
import java.util.Set;

public class WorkOrderBudgetInfo {

    private WorkOrderBudgetClient client;
    private WorkOrderBudgetVehicle vehicle;
    private String description;
    private Set<WorkOrderBudgetItem> items = new HashSet<>();

    public WorkOrderBudgetInfo() {
    }

    public WorkOrderBudgetInfo(WorkOrderBudgetClient client, WorkOrderBudgetVehicle vehicle,
                               Set<WorkOrderBudgetItem> items) {
        this(client, vehicle, null, items);
    }

    public WorkOrderBudgetInfo(WorkOrderBudgetClient client, WorkOrderBudgetVehicle vehicle,
                               String description, Set<WorkOrderBudgetItem> items) {
        this.client = client;
        this.vehicle = vehicle;
        this.description = description;
        setItems(items);
    }

    public WorkOrderBudgetClient getClient() {
        return client;
    }

    public void setClient(WorkOrderBudgetClient client) {
        this.client = client;
    }

    public WorkOrderBudgetVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(WorkOrderBudgetVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<WorkOrderBudgetItem> getItems() {
        return items;
    }

    public void setItems(Set<WorkOrderBudgetItem> items) {
        this.items = items == null ? new HashSet<>() : new HashSet<>(items);
    }
}
