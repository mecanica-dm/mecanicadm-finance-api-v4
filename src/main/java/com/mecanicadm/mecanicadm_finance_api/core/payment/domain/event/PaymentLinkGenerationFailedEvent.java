package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PaymentLinkGenerationFailedEvent(
        UUID workOrderId,
        String externalReference,
        String reason,
        UUID messageId,
        ZonedDateTime ocurredAt
) implements MessageHeader {

    public PaymentLinkGenerationFailedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
        ocurredAt = ocurredAt != null ? ocurredAt : ZonedDateTime.now(ZoneOffset.UTC);
    }
}
