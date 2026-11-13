package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.PaymentStatuses;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentCanceledEvent;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CancelMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.VoidUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public class CancelMercadoPagoPaymentUseCase implements VoidUseCase<CancelMercadoPagoPaymentCommand> {

    private final PaymentGateway paymentGateway;
    private final MercadoPagoOrderGateway orderGateway;
    private final MercadoPagoAccessTokenProvider accessTokenProvider;
    private final EventPublisher eventPublisher;

    public CancelMercadoPagoPaymentUseCase(PaymentGateway paymentGateway,
                                           MercadoPagoOrderGateway orderGateway,
                                           MercadoPagoAccessTokenProvider accessTokenProvider,
                                           EventPublisher eventPublisher) {
        this.paymentGateway = paymentGateway;
        this.orderGateway = orderGateway;
        this.accessTokenProvider = accessTokenProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void execute(CancelMercadoPagoPaymentCommand command) {
        Optional<Payment> found = paymentGateway.findLatestByExternalReference(command.workOrderId().toString());
        if (found.isEmpty()) {
            publish(command, null);
            return;
        }

        Payment payment = found.get();
        if (PaymentStatuses.isCanceled(payment.status())) {
            publish(command, payment);
            return;
        }
        if (PaymentStatuses.isConfirmed(payment.status()) || PaymentStatuses.isRefunded(payment.status())) {
            throw new PaymentException.PaymentAlreadyConfirmed();
        }

        MercadoPagoOrder order = cancelAtProvider(payment);
        Payment canceled = paymentGateway.save(payment.withProviderUpdate(PaymentStatuses.CANCELED, order));
        publish(command, canceled);
    }

    private MercadoPagoOrder cancelAtProvider(Payment payment) {
        if (payment.mercadoPagoOrderId() == null) {
            return null;
        }
        try {
            return orderGateway.cancelOrder(payment.mercadoPagoOrderId(), accessTokenProvider.accessToken(), "cancel-" + payment.id());
        } catch (PaymentException.MercadoPagoProviderError error) {
            if (error.getStatus() == 409 && error.getArgs().length > 0 && String.valueOf(error.getArgs()[0]).contains("order_already_canceled")) {
                return null;
            }
            throw error;
        }
    }

    private void publish(CancelMercadoPagoPaymentCommand command, Payment payment) {
        eventPublisher.publish(new PaymentCanceledEvent(command.workOrderId(), payment == null ? null : payment.id(), null));
    }
}
