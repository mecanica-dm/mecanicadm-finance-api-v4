package com.mecanicadm.mecanicadm_api.core.workorder.domain.event;

import com.mecanicadm.mecanicadm_api.core.shared.domain.event.MessageHeader;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class BudgetUpdatedEvent implements MessageHeader {
    private UUID messageId = UUID.randomUUID();
    private UUID workOrderId;
    private ZonedDateTime ocurredAt = ZonedDateTime.now(ZoneOffset.UTC);
    private BigDecimal totalPrice;
    private String observation;

    public BudgetUpdatedEvent() {
    }

    public BudgetUpdatedEvent(UUID workOrderId, BigDecimal totalPrice, String observation) {
        this.workOrderId = workOrderId;
        this.totalPrice = totalPrice;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
