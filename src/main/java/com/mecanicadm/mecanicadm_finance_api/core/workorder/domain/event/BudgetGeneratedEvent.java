package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.math.BigDecimal;
import java.util.UUID;

public class BudgetGeneratedEvent implements MessageHeader {
    private UUID messageId = UUID.randomUUID();
    private UUID workOrderId;
    private BigDecimal totalPrice;

    public BudgetGeneratedEvent() {
    }

    public BudgetGeneratedEvent(UUID workOrderId, BigDecimal totalPrice) {
        this.workOrderId = workOrderId;
        this.totalPrice = totalPrice;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
