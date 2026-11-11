package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.LaborExecutionStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WorkOrderSnapshot(
        BigDecimal totalPrice,
        WorkOrderBudgetStatus status,
        String observation,
        String description,
        Client client,
        Vehicle vehicle,
        List<MaterialItem> materialItems,
        List<LaborItem> laborItems
) {

    public record Client(
            UUID id,
            String name,
            String email,
            String document,
            String phone
    ) {
    }

    public record Vehicle(
            String licensePlate,
            String model,
            String brand,
            Short modelYear
    ) {
    }

    public record MaterialItem(
            UUID id,
            UUID materialId,
            int quantity,
            String name,
            String brand,
            String description,
            BigDecimal price,
            String type
    ) {
    }

    public record LaborItem(
            UUID id,
            UUID workOrderId,
            UUID laborId,
            LocalDateTime executionStartAt,
            LocalDateTime executionEndAt,
            LaborExecutionStatus status,
            String name,
            BigDecimal price
    ) {
    }
}
