package com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event;

import java.util.UUID;

public class FinanceCommandRejectedEvent implements MessageHeader {
    private UUID messageId = UUID.randomUUID();
    private UUID workOrderId;

    private String eventType;
    private String exception;
    private String message;

    public FinanceCommandRejectedEvent() {
    }

    public FinanceCommandRejectedEvent(UUID workOrderId, String eventType, String exception, String message) {
        this.workOrderId = workOrderId;
        this.eventType = eventType;
        this.exception = exception;
        this.message = message;
    }

    public FinanceCommandRejectedEvent(UUID workOrderId, String eventType, String exception, String message, UUID messageId) {
        this(workOrderId, eventType, exception, message);
        if (messageId != null) {
            this.messageId = messageId;
        }
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
