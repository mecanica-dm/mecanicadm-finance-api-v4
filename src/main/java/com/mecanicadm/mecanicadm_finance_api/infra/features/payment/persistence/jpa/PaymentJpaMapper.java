package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.entity.PaymentJpaEntity;

import static java.util.Objects.isNull;

public class PaymentJpaMapper {

    private PaymentJpaMapper() {
    }

    public static Payment toDomain(PaymentJpaEntity entity) {
        if (isNull(entity)) return null;
        return new Payment(
                entity.getId(),
                entity.getExternalReference(),
                entity.getIdempotencyKey(),
                entity.getMercadoPagoOrderId(),
                entity.getMercadoPagoPaymentId(),
                entity.getStatus(),
                entity.getStatusDetail(),
                entity.getAmount(),
                entity.getInstallments(),
                entity.getPaymentMethodId(),
                entity.getPayerEmail(),
                entity.getProviderResponse(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static PaymentJpaEntity toEntity(Payment domain) {
        if (isNull(domain)) return null;
        return new PaymentJpaEntity(
                domain.id(),
                domain.externalReference(),
                domain.idempotencyKey(),
                domain.mercadoPagoOrderId(),
                domain.mercadoPagoPaymentId(),
                domain.status(),
                domain.statusDetail(),
                domain.amount(),
                domain.installments(),
                domain.paymentMethodId(),
                domain.payerEmail(),
                domain.providerResponse(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
