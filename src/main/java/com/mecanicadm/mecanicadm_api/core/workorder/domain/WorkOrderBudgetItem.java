package com.mecanicadm.mecanicadm_api.core.workorder.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class WorkOrderBudgetItem {

    public static final String TYPE_MATERIAL = "MATERIAL";
    public static final String TYPE_LABOR = "LABOR";

    private UUID id;
    private String description;
    private int quantity;
    private BigDecimal price;
    private String type;

    public WorkOrderBudgetItem() {
    }

    private WorkOrderBudgetItem(UUID id, String description, int quantity, BigDecimal price, String type) {
        this.id = id;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
        this.type = type;
    }

    public static WorkOrderBudgetItem create(String description, int quantity, BigDecimal price, String type) {
        return new WorkOrderBudgetItem(UUID.randomUUID(), description, quantity, price, type);
    }

    public static WorkOrderBudgetItem restore(UUID id, String description, int quantity, BigDecimal price, String type) {
        return new WorkOrderBudgetItem(id == null ? UUID.randomUUID() : id, description, quantity, price, type);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderBudgetItem that = (WorkOrderBudgetItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
