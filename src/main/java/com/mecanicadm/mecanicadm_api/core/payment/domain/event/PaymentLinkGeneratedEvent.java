package com.mecanicadm.mecanicadm_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_api.core.shared.domain.event.MessageHeader;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PaymentLinkGeneratedEvent(
        UUID workOrderId,
        UUID paymentId,
        String paymentLink,
        BigDecimal amount,
        UUID messageId,
        ZonedDateTime ocurredAt
) implements MessageHeader {

    public PaymentLinkGeneratedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
        ocurredAt = ocurredAt != null ? ocurredAt : ZonedDateTime.now(ZoneOffset.UTC);
    }
}
