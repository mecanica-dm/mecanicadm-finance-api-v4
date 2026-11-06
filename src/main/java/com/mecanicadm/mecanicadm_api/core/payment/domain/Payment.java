package com.mecanicadm.mecanicadm_api.core.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Payment(
        UUID id,
        String externalReference,
        String idempotencyKey,
        String mercadoPagoOrderId,
        String mercadoPagoPaymentId,
        String status,
        String statusDetail,
        BigDecimal amount,
        Integer installments,
        String paymentMethodId,
        String payerEmail,
        MercadoPagoOrder providerResponse,
        Instant createdAt,
        Instant updatedAt
) {

    private static final String UNKNOWN_STATUS = "UNKNOWN";

    public static Payment create(String externalReference,
                                 String idempotencyKey,
                                 BigDecimal amount,
                                 Integer installments,
                                 String paymentMethodId,
                                 String payerEmail,
                                 MercadoPagoOrder providerResponse) {
        Instant now = Instant.now();
        MercadoPagoOrder.Payment providerPayment = firstPayment(providerResponse);

        return new Payment(
                UUID.randomUUID(),
                externalReference,
                idempotencyKey,
                providerResponse == null ? null : providerResponse.getId(),
                providerPayment == null ? null : providerPayment.getId(),
                resolveStatus(providerResponse),
                providerResponse == null ? null : providerResponse.getStatusDetail(),
                amount,
                installments,
                providerPayment == null || providerPayment.getPaymentMethod() == null
                        ? paymentMethodId
                        : providerPayment.getPaymentMethod().getId(),
                payerEmail,
                providerResponse,
                now,
                now
        );
    }

    private static String resolveStatus(MercadoPagoOrder providerResponse) {
        if (providerResponse == null || providerResponse.getStatus() == null
                || providerResponse.getStatus().isBlank()) {
            return UNKNOWN_STATUS;
        }
        return providerResponse.getStatus();
    }

    private static MercadoPagoOrder.Payment firstPayment(MercadoPagoOrder providerResponse) {
        if (providerResponse == null || providerResponse.getTransactions() == null) {
            return null;
        }
        List<MercadoPagoOrder.Payment> payments = providerResponse.getTransactions().getPayments();
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        return payments.get(0);
    }
}
