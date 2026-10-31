package com.mecanicadm.mecanicadm_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_api.core.material.domain.enums.MaterialType;
import com.mecanicadm.mecanicadm_api.core.shared.domain.event.MessageHeader;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.LaborExecutionStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Legacy inbound event emitted by the work order service.
 *
 * <p>It is kept as an anti-corruption adapter so the finance API can receive the data needed to
 * build a {@code WorkOrderBudget}. The {@code workOrderId} is only an external correlation key;
 * there is no {@code WorkOrder} aggregate in this service.</p>
 */
public record WorkOrderCreatedEvent(
        UUID workOrderId,
        BigDecimal totalPrice,
        WorkOrderBudgetStatus status,
        String observation,
        String description,
        Client client,
        Vehicle vehicle,
        List<MaterialItem> materialItems,
        List<LaborItem> laborItems,
        UUID messageId,
        ZonedDateTime ocurredAt
) implements MessageHeader {

    public WorkOrderCreatedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
        ocurredAt = ocurredAt != null ? ocurredAt : ZonedDateTime.now(ZoneOffset.UTC);
    }

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
            MaterialType type
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
