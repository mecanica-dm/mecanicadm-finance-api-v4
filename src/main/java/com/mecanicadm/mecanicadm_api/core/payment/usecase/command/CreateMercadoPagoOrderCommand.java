package com.mecanicadm.mecanicadm_api.core.payment.usecase.command;

import com.mecanicadm.mecanicadm_api.core.payment.domain.CardData;

import java.math.BigDecimal;

public record CreateMercadoPagoOrderCommand(
        String idempotencyKey,
        String externalReference,
        String description,
        BigDecimal totalAmount,
        String payerEmail,
        String payerFirstName,
        String payerLastName,
        Integer installments,
        String paymentMethodId,
        String paymentMethodType,
        CardData card
) {
}
