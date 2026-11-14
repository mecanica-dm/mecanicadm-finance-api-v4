package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.util.UUID;

public record PaymentCanceledEvent(
        UUID workOrderId,
        UUID paymentId,
        UUID messageId
) implements MessageHeader {

    public PaymentCanceledEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
    }
}
