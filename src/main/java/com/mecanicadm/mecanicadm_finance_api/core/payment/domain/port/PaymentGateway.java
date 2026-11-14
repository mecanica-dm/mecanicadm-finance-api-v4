package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentGateway {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findLatestByExternalReference(String externalReference);

    List<Payment> findPendingForVerification(Instant updatedBefore, int limit);
}
