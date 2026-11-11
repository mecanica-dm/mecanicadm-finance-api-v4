package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.util.UUID;

public record PaymentLinkGenerationFailedEvent(
        UUID workOrderId,
        String externalReference,
        String reason,
        UUID messageId
) implements MessageHeader {

    public PaymentLinkGenerationFailedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
    }
}
