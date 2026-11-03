package com.mecanicadm.mecanicadm_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_api.core.shared.domain.event.MessageHeader;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class BudgetSentForDecisionEvent implements MessageHeader {
    private UUID messageId = UUID.randomUUID();
    private UUID workOrderId;
    private ZonedDateTime ocurredAt = ZonedDateTime.now(ZoneOffset.UTC);

    public BudgetSentForDecisionEvent() {
    }

    public BudgetSentForDecisionEvent(UUID workOrderId) {
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

    @Override
    public ZonedDateTime ocurredAt() {
        return ocurredAt;
    }

    public ZonedDateTime getOcurredAt() {
        return ocurredAt;
    }
}
