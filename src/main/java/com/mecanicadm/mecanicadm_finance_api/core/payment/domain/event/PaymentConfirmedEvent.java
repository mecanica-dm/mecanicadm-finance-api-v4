package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PaymentConfirmedEvent(
        UUID workOrderId,
        UUID paymentId,
        String mercadoPagoPaymentId,
        BigDecimal amount,
        UUID messageId,
        ZonedDateTime ocurredAt
) implements MessageHeader {

    public PaymentConfirmedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
        ocurredAt = ocurredAt != null ? ocurredAt : ZonedDateTime.now(ZoneOffset.UTC);
    }
}
