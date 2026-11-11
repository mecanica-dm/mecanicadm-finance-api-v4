package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.util.UUID;

public class BudgetRejectedEvent implements MessageHeader {
    private UUID messageId = UUID.randomUUID();
    private UUID workOrderId;
    private String observation;

    public BudgetRejectedEvent() {
    }

    public BudgetRejectedEvent(UUID workOrderId, String observation) {
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

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
