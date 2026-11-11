package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentLinkGeneratedEvent(
        UUID workOrderId,
        UUID paymentId,
        String paymentLink,
        BigDecimal amount,
        UUID messageId
) implements MessageHeader {

    public PaymentLinkGeneratedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
    }
}
