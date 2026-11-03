package com.mecanicadm.mecanicadm_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_api.core.shared.domain.event.MessageHeader;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class BudgetChangesRequestedEvent implements MessageHeader {
    private UUID messageId = UUID.randomUUID();
    private UUID workOrderId;
    private ZonedDateTime ocurredAt = ZonedDateTime.now(ZoneOffset.UTC);
    private String observation;

    public BudgetChangesRequestedEvent() {
    }

    public BudgetChangesRequestedEvent(UUID workOrderId, String observation) {
        this.workOrderId = workOrderId;
        this.observation = observation;
    }

    @Override
    public UUID messageId() {
        return messageId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    @Override
    public UUID workOrderId() {
        return workOrderId;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(UUID workOrderId) {
        this.workOrderId = workOrderId;
    }

    @Override
    public ZonedDateTime ocurredAt() {
        return ocurredAt;
    }

    public ZonedDateTime getOcurredAt() {
        return ocurredAt;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
