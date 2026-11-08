package com.mecanicadm.mecanicadm_finance_api.core.payment.domain;

public record MercadoPagoOrderData(
        String externalReference,
        String description,
        String totalAmount,
        String payerEmail,
        String payerFirstName,
        String payerLastName,
        String paymentMethodId,
        String paymentMethodType,
        String cardToken,
        Integer installments
) {
}
