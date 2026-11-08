package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.persistence.entity;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "work_order_budgets")
public class WorkOrderBudgetJpaEntity {

    @Id
    @Column(name = "work_order_id", nullable = false, updatable = false)
    private UUID workOrderId;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkOrderBudgetStatus status;

    @Column(name = "observation")
    private String observation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "work_order_budget_info", columnDefinition = "jsonb")
    private WorkOrderBudgetInfo workOrderBudgetInfo;

    protected WorkOrderBudgetJpaEntity() {
    }

    public WorkOrderBudgetJpaEntity(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation) {
        this(workOrderId, totalPrice, status, observation, null);
    }

    public WorkOrderBudgetJpaEntity(UUID workOrderId, BigDecimal totalPrice, WorkOrderBudgetStatus status, String observation,
                                    WorkOrderBudgetInfo workOrderBudgetInfo) {
        this.workOrderId = workOrderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.observation = observation;
        this.workOrderBudgetInfo = workOrderBudgetInfo;
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

    public WorkOrderBudgetInfo getWorkOrderBudgetInfo() {
        return workOrderBudgetInfo;
    }
}
