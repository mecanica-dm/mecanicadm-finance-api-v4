package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentConfirmedEvent(
        UUID workOrderId,
        UUID paymentId,
        String mercadoPagoPaymentId,
        BigDecimal amount,
        UUID messageId
) implements MessageHeader {

    public PaymentConfirmedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
    }
}
