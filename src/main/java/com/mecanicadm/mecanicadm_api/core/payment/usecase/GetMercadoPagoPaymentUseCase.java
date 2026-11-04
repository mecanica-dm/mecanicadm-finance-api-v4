package com.mecanicadm.mecanicadm_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_api.core.payment.exception.PaymentException;

import java.util.UUID;

public class GetMercadoPagoPaymentUseCase {

    private final PaymentGateway paymentGateway;

    public GetMercadoPagoPaymentUseCase(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public Payment execute(UUID id) {
        Payment payment = paymentGateway.findById(id)
                .orElseThrow(PaymentException.PaymentNotFound::new);

        if (payment.providerResponse() == null) {
            throw new PaymentException.PaymentNotFound();
        }

        return payment;
    }
}
