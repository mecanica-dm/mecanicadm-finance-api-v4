package com.mecanicadm.mecanicadm_api.core.payment.domain.port;

import com.mecanicadm.mecanicadm_api.core.payment.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentGateway {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
