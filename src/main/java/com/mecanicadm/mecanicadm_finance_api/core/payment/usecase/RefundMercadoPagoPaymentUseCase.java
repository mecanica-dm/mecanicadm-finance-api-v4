package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.PaymentStatuses;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentRefundedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.RefundMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.VoidUseCase;
import org.springframework.transaction.annotation.Transactional;

public class RefundMercadoPagoPaymentUseCase implements VoidUseCase<RefundMercadoPagoPaymentCommand> {

    private final PaymentGateway paymentGateway;
    private final MercadoPagoOrderGateway orderGateway;
    private final MercadoPagoAccessTokenProvider accessTokenProvider;
    private final EventPublisher eventPublisher;

    public RefundMercadoPagoPaymentUseCase(PaymentGateway paymentGateway,
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
    public void execute(RefundMercadoPagoPaymentCommand command) {
        Payment payment = paymentGateway.findLatestByExternalReference(command.workOrderId().toString())
                .orElseThrow(PaymentException.PaymentNotFound::new);

        if (PaymentStatuses.isRefunded(payment.status())) {
            publish(command, payment);
            return;
        }
        if (!PaymentStatuses.isConfirmed(payment.status())) {
            throw new PaymentException.PaymentNotRefundable();
        }

        MercadoPagoOrder order = orderGateway.refundOrder(
                payment.mercadoPagoOrderId(), accessTokenProvider.accessToken(), "refund-" + payment.id());
        publish(command, paymentGateway.save(payment.withProviderUpdate(PaymentStatuses.REFUNDED, order)));
    }

    private void publish(RefundMercadoPagoPaymentCommand command, Payment payment) {
        eventPublisher.publish(new PaymentRefundedEvent(command.workOrderId(), payment.id(), null));
    }
}
