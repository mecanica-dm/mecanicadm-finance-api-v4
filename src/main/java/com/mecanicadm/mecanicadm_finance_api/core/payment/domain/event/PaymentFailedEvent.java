package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID workOrderId,
        UUID paymentId,
        String mercadoPagoPaymentId,
        BigDecimal amount,
        String reason,
        UUID messageId
) implements MessageHeader {

    public PaymentFailedEvent {
        messageId = messageId != null ? messageId : UUID.randomUUID();
    }
}
