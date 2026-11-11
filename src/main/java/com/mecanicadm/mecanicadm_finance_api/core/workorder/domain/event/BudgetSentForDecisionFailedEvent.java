package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.util.UUID;

public class BudgetSentForDecisionFailedEvent implements MessageHeader {
    private UUID messageId = UUID.randomUUID();
    private UUID workOrderId;

    public BudgetSentForDecisionFailedEvent() {
    }

    public BudgetSentForDecisionFailedEvent(UUID workOrderId) {
        this.workOrderId = workOrderId;
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
}
